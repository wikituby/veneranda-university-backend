package com.ispautomation.modules.router.communication;

import com.ispautomation.modules.router.entity.Router;

/**
 * Strategy interface for communicating with different router vendors.
 *
 * Implementations can be plugged in for MikroTik API, SSH, SNMP, or
 * vendor-specific protocols. This interface provides extension points
 * without coupling the core router management to any specific protocol.
 *
 * Currently a stub — real device communication will be implemented
 * when the NAS/RADIUS module is built.
 */
public interface DeviceCommunicationService {

    /**
     * Test connectivity to a router.
     *
     * @param router the router to test
     * @return true if reachable, false otherwise
     */
    boolean testConnection(Router router);

    /**
     * Synchronize router information (firmware, version, interfaces, etc.).
     *
     * @param router the router to sync
     * @return updated router state
     */
    Router synchronize(Router router);

    /**
     * Check if this implementation supports the given vendor.
     *
     * @param vendor the router vendor enum name
     * @return true if supported
     */
    boolean supportsVendor(String vendor);

    /**
     * Retrieve the vendor name this implementation handles.
     *
     * @return vendor identifier
     */
    String getVendor();
}