package com.emc.ldap;

import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.LDAPTestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

import com.unboundid.ldap.sdk.extensions.PasswordModifyExtendedRequest;
import com.unboundid.ldap.sdk.extensions.PasswordModifyExtendedResult;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Controller
public class ChangePasswordController {

    public static final String[] USER_PASSWORD_ATTRS = {"userPassword"};
    public static final String MESSAGES = "messages";
    public static final String ERROR = "error";
    public static final String RESULT_PAGE = "passwordChange";

    @Value("${url:localhost:389}") //serverIP:serverPort
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

        model.addAttribute(RESULT_PAGE, passwordChange);
        return "ChangePassword";
    }

    private List<String> validateDetails(PasswordChange passwordChange){

        System.out.println("Attempting to change password for " + passwordChange.getName() + " to " + passwordChange.getPassword());

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

        System.out.println("tooShort:" + tooShort);
        System.out.println("noUpperCase:" + noUpperCase);
        System.out.println("noLowerCase:" + noLowerCase);
        System.out.println("noNumber:" + noNumber);
        System.out.println("noSymbol:" + noSymbol);


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

            if(messages.size() > 0) {
                System.out.println("Invalid password change request");

                model.addAttribute(ERROR, true);

                model.addAttribute(MESSAGES, messages);

                return RESULT_PAGE;
            }

            System.out.println("Attempting to change password");

            final String[] parts = url.split(":",2);

            final String host = parts[0];

            final String portString = parts.length == 2 ? parts[1] : "389";

            final int port = Integer.parseInt(portString);

            final String userDn = cnprefix + passwordChange.getName() + cnsuffix;

            LDAPConnection connection = new LDAPConnection(host, port, userDn, passwordChange.getOldPassword());

            try {
                final PasswordModifyExtendedRequest request = new PasswordModifyExtendedRequest(userDn,
                        passwordChange.getOldPassword(),
                        passwordChange.getPassword());

                PasswordModifyExtendedResult result;

                try{
                    result = (PasswordModifyExtendedResult) connection.processExtendedOperation(request);

                }
                catch(LDAPException ex){
                    System.out.println("Could not change password: " + ex.getMessage());
                    System.out.println("Diagnostic message: " + ex.getDiagnosticMessage());
                    result = new PasswordModifyExtendedResult(new ExtendedResult(ex.toLDAPResult()));
                }

                if(result.getResultCode() != ResultCode.SUCCESS){

                    System.out.println("Result Code: " + result.getResultCode().toString());

                    messages.add(result.getResultCode().toString());

                    model.addAttribute(ERROR, true);

                    model.addAttribute(MESSAGES, messages);
                }
            }
            finally{
                connection.close();
            }

            return RESULT_PAGE;
        } catch (Exception e) {
            System.out.println("try to update password lead to an error: " + e.getMessage());
            model.addAttribute(MESSAGES, e.getMessage());
            model.addAttribute(ERROR, true);
            e.printStackTrace();
            return RESULT_PAGE;
        }
    }
}
