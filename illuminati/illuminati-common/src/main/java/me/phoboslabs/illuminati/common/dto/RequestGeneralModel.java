package me.phoboslabs.illuminati.common.dto;

import com.google.gson.annotations.Expose;
import me.phoboslabs.illuminati.common.util.StringObjectUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 10/07/2017.
 */
public class RequestGeneralModel {

    private static final Logger REQUEST_GENERAL_MODEL_LOGGER = LoggerFactory.getLogger(RequestGeneralModel.class);

    @Expose private String clientIp;
    @Expose private String methodName;
    @Expose private Map<String, Object> methodParams;
    @Expose private String path;
    @Expose private String anotherPath;
    @Expose private String queryString;

    private transient Method method;

    private static final List<String> CLIENT_INFO_KEY_LIST = Collections.unmodifiableList(Arrays.asList("path", "queryString", "clientIp", "anotherPath"));

    public RequestGeneralModel () {}

    public RequestGeneralModel (final Map<String, Object> requestMap) throws Exception {
        if (MapUtils.isEmpty(requestMap)) {
            final String errorMessage = "Sorry. check your requestMap variable.";
            REQUEST_GENERAL_MODEL_LOGGER.error(errorMessage);
            throw new Exception(errorMessage);
        }

        for (Map.Entry<String, Object> entry : requestMap.entrySet() ) {
            if (entry.getValue() == null) {
                REQUEST_GENERAL_MODEL_LOGGER.debug("Sorry. check your requestMap "+entry.getKey()+" key variable.");
                continue;
            }

            try {
                final Field field = this.getClass().getDeclaredField(entry.getKey());
                field.setAccessible(true);

                if ("serverPort".equals(entry.getKey())) {
                    field.setInt(this, (Integer) entry.getValue());
                } else {
                    field.set(this, entry.getValue().toString());
                }
            } catch (NoSuchFieldException ex) {
                final String errorMessage = "Sorry. check your class field. ("+ex.getMessage()+")";
                REQUEST_GENERAL_MODEL_LOGGER.error(errorMessage, ex);
                throw new Exception(errorMessage);
            } catch (IllegalAccessException ex) {
                final String errorMessage = "Sorry. check your class field permission. ("+ex.getMessage()+")";
                REQUEST_GENERAL_MODEL_LOGGER.error(errorMessage, ex);
                throw new Exception(errorMessage);
            }
        }
    }

    private static final String CLIENT_IP_KEYWORD = "clientIp";
    private static final String REMOTE_ADDR_KEYWORD = "remoteAddr";

    public void initClientInfo (final Map<String, String> clientInfoMap) {
        if (MapUtils.isEmpty(clientInfoMap)) {
            return;
        }

        for (String key : CLIENT_INFO_KEY_LIST) {
            if (clientInfoMap.containsKey(key) == false) {
                continue;
            }
            try {
                String value = clientInfoMap.get(key);

                if (CLIENT_IP_KEYWORD.equals(key) && StringObjectUtils.isValid(value) == false) {
                    value = clientInfoMap.get(REMOTE_ADDR_KEYWORD);
                }

                if (StringObjectUtils.isValid(value)) {
                    final Field field = this.getClass().getDeclaredField(key);
                    field.setAccessible(true);
                    field.set(this, value);
                }
            } catch (Exception ignore) {}
        }
    }

    public String getServerIp() {
        return this.getServerIp();
    }

    private boolean isParamValidated(final String[] paramNames, final Object[] paramValues) {
        return paramNames != null && paramNames.length > 0 && paramValues != null && paramValues.length > 0;
    }

    public void setMethod(final Method method, final String[] paramNames, final Object[] paramValues) {
        this.method = method;
        this.methodName = this.method.toString();

        try {
            if (this.isParamValidated(paramNames, paramValues)) {
                Map<String, Object> paramMap = new HashMap<>();
                for (int i=0; i<paramNames.length; i++) {
                    paramMap.put(paramNames[i], paramValues[i]);
                }
                this.methodParams = paramMap;
            }
        } catch (Exception ex) {
            REQUEST_GENERAL_MODEL_LOGGER.error("Sorry. check your class method params. ({})", ex.getMessage(), ex);
        }
    }

    public void setCustomForEnv () {
        this.setReplaceMethodName("public java.lang.Object ");
        this.setPathForGrails();
    }

    private void setReplaceMethodName(final String replaceText) {
        if (StringObjectUtils.isValid(this.methodName)) {
            this.methodName = this.methodName.replace(replaceText, "");
        }
    }

    private void setPathForGrails () {
        // It should not affect anything other than grails. Fuxx grails
        if (this.path.indexOf("/grails") == 0 && this.path.indexOf(".dispatch") > -1
                && (this.path.indexOf(".dispatch") + 9) == this.path.length()) {
            this.path = this.path.replace("/grails", "");
            this.path = this.path.substring(0, this.path.indexOf(".dispatch"));
        }
    }

    public String getMethodName () {
        return this.methodName;
    }
}
