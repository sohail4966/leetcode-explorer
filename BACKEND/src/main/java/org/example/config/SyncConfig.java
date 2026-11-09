package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sync")
public class SyncConfig {

    private String repoUrl = "https://github.com/liquidslr/leetcode-company-wise-problems.git";
    private String branch = "main";
    private int intervalHours = 6;
    private boolean enabled = true;

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public int getIntervalHours() { return intervalHours; }
    public void setIntervalHours(int intervalHours) { this.intervalHours = intervalHours; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
