package org.venosov.keycloak.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        UserModel adapter = loadedUsers.get(user.getUsername());

        if(adapter != null) {
            UserCredentialModel cred = (UserCredentialModel) credentialInput;

            try(CloseableHttpClient instance = HttpClientBuilder.create().build()) {                
                try (CloseableHttpResponse response = instance.execute(new HttpGet("https://httpbin.org/get"))) {                    
                    String json = EntityUtils.toString(response.getEntity());
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(json);
                    String url = jsonNode.get("url").asText();
                    System.out.println("VVVV url: " + url + " - status code: " + response.getStatusLine().getStatusCode());
                                        
                    // TODO check result for the given username and password 
                    if (true) {
                        // TODO extract role
                        adapter.grantRole(realm.getRole("myrole"));
                        session.userCredentialManager().updateCredential(realm, adapter, cred);
                        loadedUsers.remove(user.getUsername());

                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            session.userLocalStorage().removeUser(realm, user);
        }

        return false;
    }
}
