package org.venosov.keycloak.migration;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ExternalUserStorageProvider implements UserStorageProvider, CredentialInputValidator, UserLookupProvider {
    protected KeycloakSession session;
    protected ComponentModel model;
    // map of loaded users in this transaction
    protected Map<String, UserModel> loadedUsers = new HashMap<>();

    public ExternalUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;
        UserCredentialModel cred = (UserCredentialModel) input;
        // TODO check external password
        String password = "password";

        return password.equals(cred.getValue());
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return session.userLocalStorage().getUserById(id, realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return session.userLocalStorage().getUserByEmail(email, realm);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        UserModel adapter = loadedUsers.get(username);

        if (adapter == null) {
            // TODO check external user
            if (true) {
                adapter = createAdapter(realm, username);
                loadedUsers.put(username, adapter);
            }
        }

        return adapter;
    }

    protected UserModel createAdapter(RealmModel realm, String username) {
        UserModel local = session.userLocalStorage().getUserByUsername(username, realm);

        if (local == null) {
            local = session.userLocalStorage().addUser(realm, username);
            local.setEnabled(true);
            local.setFederationLink(model.getId());
            // TODO check external role
            local.grantRole(realm.getRole("myrole"));
        }

        return new UserModelDelegate(local);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }
}
