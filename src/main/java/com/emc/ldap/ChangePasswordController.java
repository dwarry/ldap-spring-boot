package com.emc.ldap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.encoding.LdapShaPasswordEncoder;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.security.crypto.keygen.KeyGenerators;

import javax.naming.Context;
import javax.naming.directory.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Controller
public class ChangePasswordController {

    public static final String[] USER_PASSWORD_ATTRS = {"userPassword"};

    @Value("${url:localhost:3890}") //serverIP:serverPort
    private String url;

    @Value("${ssl:false}")
    private boolean ssl;

    @Value("${cnsuffix:, ou=users,dc=example,dc=com}")
    private String cnsuffix;

    @Value("${cnprefix:cn=}")
    private String cnprefix;

    @Value("#{'${excludes:it.admin;admin}'.split(';')}")
    private List<String> excludes;

    @RequestMapping("/")
    public String changePassword(Model model) {

        PasswordChange passwordChange = new PasswordChange();
        passwordChange.setName("");
        passwordChange.setOldPassword("");
        passwordChange.setPassword("");
        passwordChange.setPasswordRepeat("");
        passwordChange.setCnprefix(cnprefix);
        passwordChange.setCnsuffix(cnsuffix);
        passwordChange.setUrl(url);

        model.addAttribute("passwordChange", passwordChange);
        return "ChangePassword";
    }

    private List<String> validateDetails(PasswordChange passwordChange){

        final List<String> messages = new ArrayList<>();

        if(excludes.contains(passwordChange.getName().trim())) {
            messages.add("it is not allowed to change the password of this user, see exclude list");

            return messages;
        }

        String password = passwordChange.getPassword();

        if(!password.equals(passwordChange.getPasswordRepeat())) {
            messages.add("New password values do not match");

            return messages;
        }

        boolean tooShort = password.length() < 12;

        boolean noUpperCase = true;

        boolean noLowerCase = true;

        boolean noNumber = true;

        boolean noSymbol = true;

        for (int i = 0, n = password.length(); i < n; ++i){
            char ch = password.charAt(i);

            if(Character.isUpperCase(ch)){
                noUpperCase = false;
            }
            else if(Character.isLowerCase(ch)){
                noLowerCase = false;
            }
            else if(Character.isDigit(ch)){
                noNumber = false;
            }
            else {
                noSymbol = false;
            }
        }

        if(tooShort){
            messages.add("Password must be at least 12 characters long.");
        }

        if(noUpperCase){
            messages.add("Password must contain at least one upper-case character.");
        }

        if(noLowerCase){
            messages.add("Password must contain at least one lower-case character.");
        }

        if(noNumber){
            messages.add("Password must contain at least one number.");
        }

        if(noSymbol){
            messages.add("Password must contain at least one symbol.");
        }

        return messages;
    }
    @RequestMapping(value="/passwordChange", method= RequestMethod.POST)
    public String passwordChangeSubmit(@ModelAttribute PasswordChange passwordChange, Model model) {

        System.out.println("execute password change request to ldap...");
        System.out.println("url:" + url);
        System.out.println("ssl:" + ssl);
        System.out.println("cnsuffix:" + cnsuffix);
        System.out.println("cnprefix:" + cnprefix);
        System.out.println("the request will be: " + cnprefix + "YOUR-NAME" + cnsuffix);

        try {
            final List<String> messages = validateDetails(passwordChange);

            if(messages == null){

                model.addAttribute("error", true);

                model.addAttribute("message", messages);

                return "passwordChange";
            }

            final Hashtable<String, String> ldapEnv = new Hashtable<>(11);

            ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

            ldapEnv.put(Context.PROVIDER_URL,  "ldap://" + url);

            ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");

            final String principal = cnprefix + passwordChange.getName() + cnsuffix;

            ldapEnv.put(Context.SECURITY_PRINCIPAL, principal);

            ldapEnv.put(Context.SECURITY_CREDENTIALS, passwordChange.getOldPassword());

            if(ssl) {
                ldapEnv.put(Context.SECURITY_PROTOCOL, "ssl");
            }

            System.out.println("updating password... using " + "ldap://" + url + " with " + principal);

            final DirContext ldapContext = new InitialDirContext(ldapEnv);

            final Attributes entry = ldapContext.getAttributes(principal, USER_PASSWORD_ATTRS);

            System.out.println("Retrieved " + entry.size() + "attributes for " + principal);

            final Attribute oldPassword = entry.get(USER_PASSWORD_ATTRS[0]);

            System.out.println("Found a userPassword entry: " + (oldPassword == null ? "no" : "yes"));

            final BytesKeyGenerator saltGenerator = KeyGenerators.secureRandom(4);

            final byte[] salt = saltGenerator.generateKey();

            final LdapShaPasswordEncoder saltedPasswordEncoder =
                    new LdapShaPasswordEncoder();

            final String saltedPasswordHash = saltedPasswordEncoder.encodePassword(passwordChange.getPassword(), salt);

            final ModificationItem[] mods = new ModificationItem[2];

            mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, oldPassword);

            mods[1] = new ModificationItem(DirContext.ADD_ATTRIBUTE,
                    new BasicAttribute(USER_PASSWORD_ATTRS[0], saltedPasswordHash));

            ldapContext.modifyAttributes(principal, mods);

            System.out.println("password changed");

            model.addAttribute("success", true);

            return "passwordChange";
        } catch (Exception e) {
            System.out.println("try to update password lead to an error: " + e.getMessage());
            model.addAttribute("message", e.getMessage());
            model.addAttribute("error", true);
            e.printStackTrace();
            return "passwordChange";
        }
    }
}
