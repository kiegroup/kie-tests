package org.kie.tests.wb.eap.security;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

public class UserPassCallbackHandler implements CallbackHandler {

    private final String[] credentials;

    public UserPassCallbackHandler(String[] credentials) {
        this.credentials = credentials;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback current : callbacks) {
            if (current instanceof NameCallback) {
                NameCallback ncb = (NameCallback) current;
                ncb.setName(credentials[0]);
            } else if (current instanceof PasswordCallback) {
                PasswordCallback pcb = (PasswordCallback) current;
                pcb.setPassword(credentials[1].toCharArray());
            } else if (current instanceof RealmCallback) {
                RealmCallback realmCallback = (RealmCallback) current;
                realmCallback.setText(realmCallback.getDefaultText());
            } else {
                throw new UnsupportedCallbackException(current);
            }
        }

    }

}
