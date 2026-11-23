
package com.example.library.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library")
public class LibraryProperties {

    @Valid
    private Borrowing borrowing = new Borrowing();

    public Borrowing getBorrowing() {
        return borrowing;
    }

    public void setBorrowing(Borrowing borrowing) {
        this.borrowing = borrowing;
    }

    public static class Borrowing {

        private boolean enabled = true;

        @Min(value = 1, message = "Maximum active loans must be at least 1")
        private int maxActivePerClient = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxActivePerClient() {
            return maxActivePerClient;
        }

        public void setMaxActivePerClient(int maxActivePerClient) {
            this.maxActivePerClient = maxActivePerClient;
        }
    }
}

