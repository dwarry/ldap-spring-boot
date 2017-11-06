package com.emc.ldap;

import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.extensions.PasswordModifyExtendedRequest;
import com.unboundid.ldap.sdk.extensions.PasswordModifyExtendedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ChangePasswordController {

    public static final String[] USER_PASSWORD_ATTRS = {"userPassword"};
    public static final String MESSAGES = "messages";
    public static final String ERROR = "error";
    public static final String RESULT_PAGE = "passwordChange";

    private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);

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

        LOG.info("Validating new password for {}", passwordChange.getName());

        try {
            final List<String> messages = validateDetails(passwordChange);

            if(messages.size() > 0) {
                LOG.info("Invalid password change request");
                LOG.debug("Password failed the following tests: ", (Object)messages);

                model.addAttribute(ERROR, true);

                model.addAttribute(MESSAGES, messages);

                return RESULT_PAGE;
            }

            LOG.info("New password passed policy checks.");

            final String[] parts = url.split(":",2);

            final String host = parts[0];

            final String portString = parts.length == 2 ? parts[1] : "389";

            final int port = Integer.parseInt(portString);

            final String userDn = cnprefix + passwordChange.getName() + cnsuffix;

            LOG.debug("Connecting to LDAP: {} binding to {}", url, userDn);

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
                    LOG.info("Could not change password - request failed");
                    LOG.error("LDAP Exception", ex);
                    result = new PasswordModifyExtendedResult(new ExtendedResult(ex.toLDAPResult()));
                }

                if(result.getResultCode() != ResultCode.SUCCESS){

                    LOG.debug("Result Code: {}", result.getResultCode());

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
            LOG.error("Attempt to update password failed", e);

            model.addAttribute(MESSAGES, new String[]{ "Unexpected error - please contact Support" });

            model.addAttribute(ERROR, true);

            return RESULT_PAGE;
        }
    }
}
