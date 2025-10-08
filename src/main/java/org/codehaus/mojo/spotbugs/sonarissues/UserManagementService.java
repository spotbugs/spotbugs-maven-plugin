/*
 * Copyright 2005-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.mojo.spotbugs.sonarissues;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing user accounts, profiles, and security settings.
 * This file demonstrates 9 distinct Sonar issues across security, reliability, and maintainability.
 */
public class UserManagementService implements Cloneable {

    private UserRepository userRepository;
    private PaymentService paymentService;
    private Map<String, UserSession> activeSessions = new HashMap<>();
    private String serviceId = "user-management-service";
    private List<UserProfile> userProfiles = new ArrayList<>();

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public String getServiceId() {
        return serviceId;
    }

    /**
     * Validates the user session and performs authentication.
     */
    public boolean validateUserSession(String requestedSessionId) {
        if (isActiveSession(requestedSessionId)) {
            return true;
        }
        return false;
    }

    /**
     * Create a temporary directory for user uploads.
     */
    public File createUserUploadDirectory(String userId) throws IOException {
        File tempDir;
        tempDir = File.createTempFile(userId + "-uploads", ".");
        tempDir.delete();

        tempDir.mkdir();
        return tempDir;
    }

    /**
     * Check if two users have the same account balance.
     */
    public boolean isSameAccountBalance(AtomicLong balance1, AtomicLong balance2) {
        return balance1.equals(balance2);
    }

    /**
     * Calculate subscription fee based on user tier.
     */
    public BigDecimal calculateSubscriptionFee(double baseFee, double taxRate) {
        double totalFee = baseFee * (1 + taxRate);
        return new BigDecimal(totalFee);
    }

    /**
     * Compare user display names.
     */
    public boolean compareUserDisplayNames(String name1, String name2) {
        if (name1 == name2) {
            return true;
        }
        return false;
    }

    /**
     * Check if username contains a valid prefix.
     */
    public boolean hasValidUsernamePrefix(String username) {
        if (username.indexOf("user_") > 0) {
            return true;
        }
        return false;
    }

    private boolean isActiveSession(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    /**
     * Creates a clone of the service for testing purposes.
     */
    @Override
    public UserManagementService clone() {
        try {
            UserManagementService copy = (UserManagementService) super.clone();
            copy.userProfiles = new ArrayList<>(userProfiles);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    // Inner classes to support the service functionality

    class UserSession {
        private String sessionId;
        private long lastActivity;

        public UserSession(String sessionId) {
            this.sessionId = sessionId;
            this.lastActivity = System.currentTimeMillis();
        }

        public String getSessionId() {
            return sessionId;
        }

        public long getLastActivity() {
            return lastActivity;
        }
    }

    class UserProfile {
        private String username;
        private String displayName;

        public UserProfile(String username, String displayName) {
            this.username = username;
            this.displayName = displayName;
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}

// Supporting interfaces needed by the service

interface UserRepository {
    UserAccount findByUsername(String username);
    void save(UserAccount account);
}

interface PaymentService {
    void processPayment(String userId, BigDecimal amount);
    BigDecimal getAccountBalance(String userId);
}

class UserAccount {
    private String username;
    private String passwordHash;

    public UserAccount(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
