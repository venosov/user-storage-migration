package org.venosov.keycloak.migration;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import java.util.HashMap;
import java.util.Map;

public class ExternalUserStorageProvider implements UserStorageProvider, UserLookupProvider {
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
            UserCredentialModel creds = new UserCredentialModel();
            creds.setType(CredentialRepresentation.PASSWORD);
            // TODO check external role
            local.grantRole(realm.getRole("myrole"));
            // TODO check external password
            creds.setValue("password");
            session.userCredentialManager().updateCredential(realm, local, creds);
        }

        return local;
    }
}
