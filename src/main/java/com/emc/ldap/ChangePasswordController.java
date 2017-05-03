package com.emc.ldap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.naming.Context;
import javax.naming.directory.*;
import java.util.Hashtable;

@Controller
public class ChangePasswordController {

    @Value("${baseName:dc=example,dc=com}")
    String baseName;

    @Value("${providerUrl:localhost:3890}") //serverIP:serverPort
    String providerUrl;

    @Value("${ssl:false}")
    boolean ssl;

    @Value("${cnsuffix:, ou=users,dc=example,dc=com}")
    String cnsuffix;


    @RequestMapping("/")
    public String changePassword(Model model) {

        PasswordChange passwordChange = new PasswordChange();
        passwordChange.setName("");
        passwordChange.setPassword("");
        passwordChange.setPasswordRepeat("");
        model.addAttribute("passwordChange", passwordChange);
        return "ChangePassword";
    }

    @RequestMapping(value="/passwordChange", method= RequestMethod.POST)
    public String passwordChangeSubmit(@ModelAttribute PasswordChange passwordChange, Model model) {

        try {
            if(!passwordChange.getPassword().equals(passwordChange.getPasswordRepeat())) {
                model.addAttribute("error", true);
                model.addAttribute("message", "password and repeation do not fit");
                return "passwordChange";
            }

            Hashtable ldapEnv = new Hashtable(11);
            ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            ldapEnv.put(Context.PROVIDER_URL,  "ldap://" + providerUrl);
            ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
            //String principal = "cn=" + passwordChange.getName() + ", ou=users,dc=example,dc=com";
            String principal = "cn=" + passwordChange.getName() + cnsuffix;
            ldapEnv.put(Context.SECURITY_PRINCIPAL, principal);
            ldapEnv.put(Context.SECURITY_CREDENTIALS, passwordChange.getOldPassword());
            if(ssl) {
                ldapEnv.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            System.out.println("updating password... using " + "ldap://" + providerUrl + " with " + principal);
            DirContext ldapContext = new InitialDirContext(ldapEnv);
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute("userPassword", passwordChange.getPassword()));
            ldapContext.modifyAttributes(principal, mods);
            System.out.println("password changed");
            model.addAttribute("success", true);
            return "passwordChange";
        } catch (Exception e) {
            System.out.println("update password error: " + e.getMessage());
            model.addAttribute("message", e.getMessage());
            model.addAttribute("error", true);
            e.printStackTrace();
            return "passwordChange";
        }
    }
}
