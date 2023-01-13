package com.breeze.breezeconnect;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HttpDeleteWithEntity extends HttpEntityEnclosingRequestBase {
    public final static String METHOD_NAME = "DELETE";

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
