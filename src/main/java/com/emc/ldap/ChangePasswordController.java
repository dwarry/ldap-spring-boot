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
import java.util.List;

@Controller
public class ChangePasswordController {

    @Value("${url:localhost:3890}") //serverIP:serverPort
    String url;

    @Value("${ssl:false}")
    boolean ssl;

    @Value("${cnsuffix:, ou=users,dc=example,dc=com}")
    String cnsuffix;

    @Value("#{'${excludes:it.admin;admin}'.split(';')}")
    private List<String> excludes;

    @RequestMapping("/")
    public String changePassword(Model model) {

        PasswordChange passwordChange = new PasswordChange();
        passwordChange.setName("");
        passwordChange.setOldPassword("");
        passwordChange.setPassword("");
        passwordChange.setPasswordRepeat("");
        model.addAttribute("passwordChange", passwordChange);
        return "ChangePassword";
    }

    @RequestMapping(value="/passwordChange", method= RequestMethod.POST)
    public String passwordChangeSubmit(@ModelAttribute PasswordChange passwordChange, Model model) {

        System.out.println("execute password change request to ldap...");
        System.out.println("url:" + url);
        System.out.println("ssl:" + ssl);
        System.out.println("cnsuffix:" + cnsuffix);
        System.out.println("the request will be: cn=YOUR-NAME" + cnsuffix);


        try {
            if(excludes.contains(passwordChange.getName().trim())) {
                model.addAttribute("error", true);
                model.addAttribute("message", "it is not allowed to change the password of this user, see exclude list");
                return "passwordChange";
            }

            if(!passwordChange.getPassword().equals(passwordChange.getPasswordRepeat())) {
                model.addAttribute("error", true);
                model.addAttribute("message", "password and password repeat not equal");
                return "passwordChange";
            }

            Hashtable ldapEnv = new Hashtable(11);
            ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            ldapEnv.put(Context.PROVIDER_URL,  "ldap://" + url);
            ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
            //String principal = "cn=" + passwordChange.getName() + ", ou=users,dc=example,dc=com";
            String principal = "cn=" + passwordChange.getName() + cnsuffix;
            ldapEnv.put(Context.SECURITY_PRINCIPAL, principal);
            ldapEnv.put(Context.SECURITY_CREDENTIALS, passwordChange.getOldPassword());
            if(ssl) {
                ldapEnv.put(Context.SECURITY_PROTOCOL, "ssl");
            }
            System.out.println("updating password... using " + "ldap://" + url + " with " + principal);
            DirContext ldapContext = new InitialDirContext(ldapEnv);
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute("userPassword", passwordChange.getPassword()));
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
