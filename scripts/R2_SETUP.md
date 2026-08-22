# Cloudflare R2 — private lesson videos (signed URL playback)

## Fast path (automated)

From PowerShell in `backend/scripts`:

```powershell
cd C:\Users\hp\Desktop\Projects\Veneranda_University\backend\scripts
.\setup-r2.ps1
```

You will be asked for:

1. **Cloudflare Account ID** (R2 overview)
2. **Cloudflare API Token** — create at https://dash.cloudflare.com/profile/api-tokens  
   Permission: **Account → Workers R2 Storage → Edit**
3. **R2 S3 Access Key + Secret** — one dashboard click the script opens for you  
   (Cloudflare does not allow creating S3 keys via API)

The script creates the bucket, applies CORS for `http://localhost:4900`, saves `R2_*` User env vars, and can restart Quarkus.

---

## Manual path (same result)

See steps below if you prefer the dashboard only.

## 1. Create the bucket
1. Open https://dash.cloudflare.com and sign in
2. Sidebar → **R2 Object Storage** → **Create bucket**
3. Name: `veneranda-videos` (any name is fine)
4. Keep the bucket **private** (do not enable public access)

## 2. Create an API token
1. R2 → **Overview** → **Manage R2 API Tokens** → **Create API token**
2. Permissions: **Object Read & Write**
3. Apply to your bucket (or all buckets for simplicity)
4. Copy and save:
   - **Access Key ID**
   - **Secret Access Key**
   - **Endpoint** — `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`
     (Account ID is on the R2 overview page)

## 3. CORS (required so the browser can play the signed URL)
On the bucket → **Settings** → **CORS policy** → paste:

```json
[
  {
    "AllowedOrigins": ["http://localhost:4900"],
    "AllowedMethods": ["GET", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag", "Content-Length", "Content-Type"],
    "MaxAgeSeconds": 3600
  }
]
```

Add your production HTTPS origin later.

## 4. Set Windows User environment variables
PowerShell (run once):

```powershell
[Environment]::SetEnvironmentVariable('R2_ENABLED','true','User')
[Environment]::SetEnvironmentVariable('R2_ENDPOINT','https://YOUR_ACCOUNT_ID.r2.cloudflarestorage.com','User')
[Environment]::SetEnvironmentVariable('R2_ACCESS_KEY_ID','YOUR_ACCESS_KEY_ID','User')
[Environment]::SetEnvironmentVariable('R2_SECRET_ACCESS_KEY','YOUR_SECRET_ACCESS_KEY','User')
[Environment]::SetEnvironmentVariable('R2_BUCKET','veneranda-videos','User')
[Environment]::SetEnvironmentVariable('R2_PLAYBACK_TTL_SECONDS','900','User')
```

Then **restart the Quarkus backend** so it picks up the new User env vars.

## 5. Use in the LMS
1. Open a lesson → **Videos**
2. Click **Upload private (R2)** / **Choose video file** (appears when R2 is enabled)
3. Students must **Enroll** then press **Play private video**
4. Playback uses a short-lived signed URL (default 15 minutes)

YouTube links still work for non-private content.
