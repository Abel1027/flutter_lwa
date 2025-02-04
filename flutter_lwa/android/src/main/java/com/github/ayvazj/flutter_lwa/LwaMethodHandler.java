package com.github.ayvazj.flutter_lwa;

import android.content.Context;
import androidx.annotation.NonNull;
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest;
import com.amazon.identity.auth.device.api.authorization.Scope;
import com.amazon.identity.auth.device.api.authorization.ScopeFactory;
import com.amazon.identity.auth.device.api.workflow.RequestContext;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LwaMethodHandler implements MethodChannel.MethodCallHandler {
    private static final String SCOPES_ARGUMENT = "scopes";
    private static final String GRANT_TYPE_ARGUMENT = "grantType";
    private static final String PROOFKEY_PARAMETERS_ARGUMENT = "proofKeyParameters";
    private static final String CODE_CHALLENGE_ARGUMENT = "codeChallenge";
    private static final String CODE_CHALLENGE_METHOD_ARGUMENT = "codeChallengeMethod";
    private static final String METHOD_SIGN_IN = "SIGNIN";
    private static final String METHOD_SIGN_OUT = "SIGNOUT";
    private static final String METHOD_GET_TOKEN = "GETTOKEN";
    private static final String METHOD_GET_PROFILE = "GETPROFILE";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_SCOPE_DATA = "scopeData";
    private final LwaDelegate delegate;

    LwaMethodHandler(final RequestContext requestContext, final @NonNull Context context) {
        delegate = new LwaDelegate(requestContext, context);
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        switch (call.method.toUpperCase()) {
            case METHOD_SIGN_IN:
                delegate.signIn(MainThreadResult.from(result), getScopesArgument(call), getGrantTypeArgument(call), getProofKeyParametersArgument(call));
                break;
            case METHOD_SIGN_OUT:
                delegate.signOut(MainThreadResult.from(result));
                break;
            case METHOD_GET_TOKEN:
                delegate.getToken(MainThreadResult.from(result), getScopesArgument(call));
                break;
            case METHOD_GET_PROFILE:
                delegate.getProfile(MainThreadResult.from(result));
                break;
            default:
                result.notImplemented();
        }
    }

    private List<Scope> getScopesArgument(MethodCall call) {
        final List<Scope> res = new ArrayList<>();
        if (call.hasArgument(SCOPES_ARGUMENT)) {
            List<Map> scopeList = call.argument(SCOPES_ARGUMENT);
            if (scopeList != null) {
                //noinspection unchecked
                for (Map o : scopeList) {
                    try {
                        final JSONObject jo = new JSONObject(o);
                        final JSONObject scopeData = jo.has(PARAM_SCOPE_DATA) && !jo.isNull(PARAM_SCOPE_DATA)
                                ? jo.getJSONObject(PARAM_SCOPE_DATA)
                                : null;
                        Scope s = ScopeFactory.scopeNamed(
                                jo.getString(PARAM_NAME),
                                scopeData
                        );
                        res.add(s);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return res;
    }

    private AuthorizeRequest.GrantType getGrantTypeArgument(MethodCall call) {
        if (call.hasArgument(GRANT_TYPE_ARGUMENT)) {
            String grantTypeArgument = call.argument(GRANT_TYPE_ARGUMENT);
            if (grantTypeArgument != null) {
                for (AuthorizeRequest.GrantType value : AuthorizeRequest.GrantType.values()) {
                    if (value.name().equalsIgnoreCase(grantTypeArgument)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private ProofKeyParameters getProofKeyParametersArgument(MethodCall call) {
        if (call.hasArgument(PROOFKEY_PARAMETERS_ARGUMENT)) {
            Map<String, String> proofKeyArguments = call.argument(PROOFKEY_PARAMETERS_ARGUMENT);
            String codeChallenge = null;
            String codeChallengeMethod = null;
            if (proofKeyArguments != null) {
                for (String key : proofKeyArguments.keySet()) {
                    if (key.equalsIgnoreCase(CODE_CHALLENGE_ARGUMENT)) {
                        codeChallenge = proofKeyArguments.get(key);
                    }
                    if (key.equalsIgnoreCase(CODE_CHALLENGE_METHOD_ARGUMENT)) {
                        codeChallengeMethod = proofKeyArguments.get(key);
                    }
                }
                if (codeChallenge != null && codeChallengeMethod != null) {
                    return new ProofKeyParameters(codeChallenge, codeChallengeMethod);
                }
            }
        }
        return null;
    }
}
