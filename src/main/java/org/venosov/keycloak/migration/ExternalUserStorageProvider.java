package org.venosov.keycloak.migration;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
            adapter = session.userLocalStorage().getUserByUsername(username, realm);

            if (adapter == null) {
                adapter = session.userLocalStorage().addUser(realm, username);
                adapter.setFederationLink(model.getId());
                adapter.setEnabled(true);
                loadedUsers.put(username, adapter);
            }
        }

        return adapter;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return credentialType.equals(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        UserCredentialModel cred = (UserCredentialModel) credentialInput;

        try(CloseableHttpClient instance = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = instance.execute(new HttpGet("https://httpbin.org/get"))) {
                String bodyAsString = EntityUtils.toString(response.getEntity());
                System.out.println("VVVV " + bodyAsString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO check external user
        if (true) {
            UserCredentialModel creds = new UserCredentialModel();
            creds.setType(CredentialRepresentation.PASSWORD);
            UserModel adapter = loadedUsers.get(user.getUsername());

            if(adapter != null) {
                // TODO check external role
                adapter.grantRole(realm.getRole("myrole"));
                // TODO check external password
                creds.setValue(cred.getValue());
                session.userCredentialManager().updateCredential(realm, adapter, creds);

                return true;
            }
        }

        return false;
    }
}
