package org.venosov.keycloak.migration;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class ExternalUserStorageProviderFactory implements UserStorageProviderFactory<ExternalUserStorageProvider> {
    @Override
    public ExternalUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new ExternalUserStorageProvider(session, model);
    }

    @Override
    public String getId() {
        return "user-storage-migration";
    }
}
