package com.blackducksoftware.integration.maven.goal;

import static com.blackducksoftware.integration.build.Constants.CHECK_POLICIES;
import static com.blackducksoftware.integration.build.Constants.CHECK_POLICIES_ERROR;
import static com.blackducksoftware.integration.build.Constants.CHECK_POLICIES_FINISHED;
import static com.blackducksoftware.integration.build.Constants.CHECK_POLICIES_STARTING;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.hub.api.policy.PolicyStatusItem;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.MissingUUIDException;
import com.blackducksoftware.integration.hub.exception.ProjectDoesNotExistException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.RestConnection;

@Mojo(name = CHECK_POLICIES, defaultPhase = LifecyclePhase.PACKAGE)
public class CheckPoliciesGoal extends HubMojo {
    @Override
    public void performGoal() throws MojoExecutionException, MojoFailureException {
        logger.info(String.format(CHECK_POLICIES_STARTING, getBdioFilename()));

        final HubServerConfig hubServerConfig = getHubServerConfigBuilder().build();
        try {
            final RestConnection restConnection = new RestConnection(hubServerConfig);
            final PolicyStatusItem policyStatusItem = PLUGIN_HELPER.checkPolicies(restConnection, getHubProject(),
                    getHubVersion());
            handlePolicyStatusItem(policyStatusItem);
        } catch (IllegalArgumentException | URISyntaxException | BDRestException | EncryptionException | IOException
                | ProjectDoesNotExistException | HubIntegrationException | MissingUUIDException e) {
            throw new MojoFailureException(String.format(CHECK_POLICIES_ERROR, e.getMessage()), e);
        }

        logger.info(String.format(CHECK_POLICIES_FINISHED, getBdioFilename()));
    }

}
