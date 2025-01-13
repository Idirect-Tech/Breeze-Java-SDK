package com.breeze.breezeconnect;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class ApificationBreeze {
    private String base64SessionToken;
    private String secretKey;
    private String apiKey;

    public Config config;

    public void errorException(String message) throws Exception {
        throw new Exception(message);
    }

    public JSONObject validationResponse(String payload,int status,String message) throws JSONException {
        return new JSONObject(){{
            put("Success", payload);
            put("Status", status);
            put("Error", message);
        }};
    }

    public void setSession(String apiKey, String secretKey, String base64SessionToken) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.base64SessionToken = base64SessionToken;
        config = new Config();
    }

    public static void main(String[] args) {
    }

    public static String currentTimestamp(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(System.currentTimeMillis()))+".000Z";
    }

    public static String checksumValue(JSONObject jsonData, String timeStamp, String secretKey) throws JSONException {
        String hexData = timeStamp + jsonData.toString() + secretKey;
        return DigestUtils.sha256Hex(hexData);
    }

    public JSONArray generateHeaders(JSONObject body) {
        try {
            String timestamp = currentTimestamp();
            JSONArray headersObject = new JSONArray();
            headersObject.put("token "+checksumValue(body,timestamp, this.secretKey));
            headersObject.put(timestamp);
            return headersObject;
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String makeRequest(String method, String endpoint, JSONObject requestBody, JSONArray headers) {
        try {
            int CONNECTION_TIMEOUT_MS = 10000; // Timeout in millis.
            CloseableHttpClient client = HttpClients.createDefault();
            HttpEntityEnclosingRequestBase http = null;
            switch (method) {
                case "GET":
                    http = new HttpGetWithEntity();
                    break;
                case "POST":
                    http = new HttpPost();
                    break;
                case "PUT":
                    http = new HttpPut();
                    break;
                case "DELETE":
                    http = new HttpDeleteWithEntity();
                    break;
                default:
                    this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.INVALID_REQUEST_EXCEPTION));
            }
            http.setConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
                    .setConnectTimeout(CONNECTION_TIMEOUT_MS)
                    .setSocketTimeout(CONNECTION_TIMEOUT_MS)
                    .build());
            http.setURI(URI.create( config.urls.get(Config.UrlEnum.API_URL)+ endpoint));
            http.setEntity(new StringEntity(requestBody.toString()));
            http.setHeader("Content-type", "application/json");
            http.setHeader("X-Checksum", headers.getString(0));
            http.setHeader("X-Timestamp", headers.getString(1));
            http.setHeader("X-AppKey", this.apiKey);
            http.setHeader("X-SessionToken", this.base64SessionToken);
            String responseString = "";
            try {
                CloseableHttpResponse response = client.execute(http);
                HttpEntity responseEntity = response.getEntity();
                responseString = EntityUtils.toString(responseEntity, "UTF-8");
            } finally {
                client.close();
            }
            return responseString;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getCustomerDetails(String apiSession){
        try {
            if(apiSession.trim().isEmpty() || apiSession.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.API_SESSION_ERROR));
            }
            JSONArray headers = new JSONArray();
            headers.put("");
            headers.put("");
            JSONObject body = new JSONObject("{\"SessionToken\":"+apiSession+",\"AppKey\":"+this.apiKey+"}");
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.CUST_DETAILS),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getDematHoldings(){
        try {
            JSONObject body = new JSONObject("{}");
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.DEMAT_HOLDING),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getFunds(){
        try {
            JSONObject body = new JSONObject("{}");
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.FUND),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject setFunds(String transactionType, String amount, String segment){
        try {
            if(transactionType.trim().isEmpty() || transactionType.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_TRANSACTION_TYPE));
            }
            else if(!(Arrays.asList("debit","credit")).contains(transactionType.toLowerCase())){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.TRANSACTION_TYPE_ERROR));
            }
            else if(amount.trim().isEmpty() || amount.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_AMOUNT));
            }
            else if(Integer.parseInt(amount) < 0){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.ZERO_AMOUNT_ERROR));
            }
            else if(segment.trim().isEmpty() || segment.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_SEGMENT));
            }
            JSONObject body = new JSONObject(){{
                put("transaction_type",transactionType);
                put("amount",amount);
                put("segment",segment);
            }};
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.POST),
                    config.endPoints.get(Config.EndPointEnum.FUND),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject getHistoricalData(String interval, String fromDate, String toDate, String stockCode, String exchangeCode, String productType, String expiryDate, String right, String strikePrice) {
        try {
            if(interval.trim().isEmpty() || interval.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_INTERVAL));
            }
            else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.INTERVAL_TYPES))).contains(interval.toLowerCase())) {
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.INTERVAL_TYPE_ERROR));
            }
            else if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.EXCHANGE_CODES_HIST))).contains(exchangeCode.toLowerCase())) {
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.EXCHANGE_CODE_ERROR));
            }
            else if(fromDate.trim().isEmpty() || fromDate.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_FROM_DATE));
            }
            else if(toDate.trim().isEmpty() || toDate.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_TO_DATE));
            }
            else if(stockCode.trim().isEmpty() || stockCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_STOCK_CODE));
            }
            else if(exchangeCode.equalsIgnoreCase("nfo")){
                if(productType.trim().isEmpty() || productType.isEmpty()){
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.PRODUCT_TYPE_ERROR_NFO));
                }
                else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.PRODUCT_TYPES_HIST))).contains(productType.toLowerCase())) {
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.PRODUCT_TYPE_ERROR_NFO));
                }
                else if(productType.equalsIgnoreCase("options")) {
                    if(strikePrice.trim().isEmpty() || strikePrice.isEmpty()){
                        return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_STRIKE_PRICE));
                    }
                    else if(right.trim().isEmpty() || right.isEmpty()){
                        return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_RIGHT_TYPE));
                    }
                    else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.RIGHT_TYPES))).contains(right.toLowerCase())) {
                        return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.RIGHT_TYPE_ERROR));
                    }
                }
                else if(expiryDate.trim().isEmpty() || expiryDate.isEmpty()){
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXPIRY_DATE));
                }
            }
            if(interval.equalsIgnoreCase("1minute")){
                interval = "minute";
            } else if (interval.equalsIgnoreCase("1day")) {
                interval = "day";
            }
            JSONObject body = new JSONObject();
            body.put("interval",interval);
            body.put("from_date",fromDate);
            body.put("to_date",toDate);
            body.put("stock_code",stockCode);
            body.put("exchange_code",exchangeCode);
            if(!(productType.trim().isEmpty() || productType.isEmpty())){
                body.put("product_type",productType);
            }
            if(!(expiryDate.trim().isEmpty() || expiryDate.isEmpty())){
                body.put("expiry_date",expiryDate);
            }
            if(!(strikePrice.trim().isEmpty() || strikePrice.isEmpty())){
                body.put("strike_price",strikePrice);
            }
            if(!(right.trim().isEmpty() || right.isEmpty())){
                body.put("right",right);
            }
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.HIST_CHART),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getHistoricalDatav2(String interval, String fromDate, String toDate, String stockCode, String exchangeCode, String productType, String expiryDate, String right, String strikePrice) {
        try {
            if(interval.trim().isEmpty() || interval.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_INTERVAL));
            }
            else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.INTERVAL_TYPES_HIST_V2))).contains(interval.toLowerCase())) {
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.INTERVAL_TYPE_ERROR_HIST_V2));
            }
            else if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.EXCHANGE_CODES_HIST_V2))).contains(exchangeCode.toLowerCase())) {
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.EXCHANGE_CODE_HIST_V2_ERROR));
            }
            else if(fromDate.trim().isEmpty() || fromDate.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_FROM_DATE));
            }
            else if(toDate.trim().isEmpty() || toDate.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_TO_DATE));
            }
            else if(stockCode.trim().isEmpty() || stockCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_STOCK_CODE));
            }
            else if(exchangeCode.equalsIgnoreCase("nfo") || exchangeCode.equalsIgnoreCase("ndx") || exchangeCode.equalsIgnoreCase("mcx")){
                if(productType.trim().isEmpty() || productType.isEmpty()){
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_PRODUCT_TYPE_HIST_V2));
                }
                else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.PRODUCT_TYPES_HIST_V2))).contains(productType.toLowerCase())) {
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.PRODUCT_TYPE_ERROR_HIST_V2));
                }
                else if(productType.equalsIgnoreCase("options")) {
                    if(strikePrice.trim().isEmpty() || strikePrice.isEmpty()){
                        return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_STRIKE_PRICE));
                    }
                    else if(right.trim().isEmpty() || right.isEmpty()){
                        return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_RIGHT_TYPE));
                    }
                    else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.RIGHT_TYPES))).contains(right.toLowerCase())) {
                        return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.RIGHT_TYPE_ERROR));
                    }
                }
                else if(expiryDate.trim().isEmpty() || expiryDate.isEmpty()){
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXPIRY_DATE));
                }
            }

            String query_params = "?";
            query_params = (query_params+"interval="+interval);
            query_params = (query_params+"&from_date="+fromDate);
            query_params = (query_params+"&to_date="+toDate);
            query_params = (query_params+"&stock_code="+stockCode);
            query_params = (query_params+"&exch_code="+exchangeCode);
            if(!(productType.trim().isEmpty() || productType.isEmpty())){
                query_params = (query_params+"&product_type="+productType);
            }
            if(!(expiryDate.trim().isEmpty() || expiryDate.isEmpty())){
                query_params = (query_params+"&expiry_date="+expiryDate);
            }
            if(!(strikePrice.trim().isEmpty() || strikePrice.isEmpty())){
                query_params = (query_params+"&strike_price="+strikePrice);
            }
            if(!(right.trim().isEmpty() || right.isEmpty())){
                query_params = (query_params+"&right="+right);
            }

            int CONNECTION_TIMEOUT_MS = 10000; // Timeout in millis.
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet http = null;

            String requestURL = config.urls.get(Config.UrlEnum.BREEZE_NEW_URL)+ config.endPoints.get(Config.EndPointEnum.HIST_CHART) + query_params;
            http = new HttpGet();
            http.setConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
                    .setConnectTimeout(CONNECTION_TIMEOUT_MS)
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setSocketTimeout(CONNECTION_TIMEOUT_MS)
                    .build());
            http.setURI(URI.create(requestURL));
            http.setHeader("Content-type", "application/json");
            http.setHeader("apikey", this.apiKey);
            http.setHeader("X-SessionToken", this.base64SessionToken);
            String responseString = "";
            try {
                CloseableHttpResponse response = client.execute(http);
                HttpEntity responseEntity = response.getEntity();
                responseString = EntityUtils.toString(responseEntity, "UTF-8");
            } catch (ClientProtocolException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                client.close();
            }
            return new JSONObject(responseString);
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    public JSONObject addMargin(String productType, String stockCode, String exchangeCode, String settlementId, String addAmount, String marginAmount, String openQuantity, String coverQuantity, String categoryIndexPerStock, String expiryDate, String right, String contractTag, String strikePrice, String segmentCode){
        try{
            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(!productType.trim().isEmpty() && !productType.isEmpty() && !(Arrays.asList(config.typeLists.get(Config.ListEnum.PRODUCT_TYPES)).contains(productType.toLowerCase()))){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.PRODUCT_TYPE_ERROR));
            }
            else if(!right.trim().isEmpty() && !right.isEmpty() && !(Arrays.asList(config.typeLists.get(Config.ListEnum.RIGHT_TYPES))).contains(right.toLowerCase())){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.RIGHT_TYPE_ERROR));
            }
            JSONObject body = new JSONObject();
            body.put("exchange_code",exchangeCode);
            if(!productType.trim().isEmpty() && !productType.isEmpty()){
                body.put("product_type",productType);
            }
            if(!(stockCode.trim().isEmpty() && !stockCode.isEmpty())){
                body.put("stock_code",stockCode);
            }
            if(!(coverQuantity.trim().isEmpty() && !coverQuantity.isEmpty())){
                body.put("cover_quantity",coverQuantity);
            }
            if(!(categoryIndexPerStock.trim().isEmpty() && !categoryIndexPerStock.isEmpty())){
                body.put("category_index_per_stock",categoryIndexPerStock);
            }
            if(!(contractTag.trim().isEmpty() && !contractTag.isEmpty())){
                body.put("contract_tag",contractTag);
            }
            if(!(marginAmount.trim().isEmpty() && !marginAmount.isEmpty())){
                body.put("margin_amount",marginAmount);
            }
            if(!(expiryDate.trim().isEmpty() && !expiryDate.isEmpty())){
                body.put("expiry_date",expiryDate);
            }
            if(!(right.trim().isEmpty() && !right.isEmpty())){
                body.put("right",right);
            }
            if(!(strikePrice.trim().isEmpty() && !strikePrice.isEmpty())){
                body.put("strike_price",strikePrice);
            }
            if(!(segmentCode.trim().isEmpty() && !segmentCode.isEmpty())){
                body.put("segment_code",segmentCode);
            }
            if(!(settlementId.trim().isEmpty() && !settlementId.isEmpty())){
                body.put("settlement_id",settlementId);
            }
            if(!(addAmount.trim().isEmpty() && !addAmount.isEmpty())){
                body.put("add_amount",addAmount);
            }
            if(!(openQuantity.trim().isEmpty() && !openQuantity.isEmpty())){
                body.put("open_quantity",openQuantity);
            }
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.POST),
                    config.endPoints.get(Config.EndPointEnum.MARGIN),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getMargin(String exchangeCode){
        try{
            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            JSONObject body = new JSONObject();
            body.put("exchange_code",exchangeCode);
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.MARGIN),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject placeOrder(String stockCode, String exchangeCode, String productType, String action, String orderType, String stoploss, String quantity, String price, String validity, String validityDate, String disclosedQuantity, String expiryDate, String right, String strikePrice, String userRemark){
        try{

            if(stockCode.trim().isEmpty() || stockCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_STOCK_CODE));
            }
            else if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(productType.trim().isEmpty() || productType.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_PRODUCT_TYPE));
            }
            else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.PRODUCT_TYPES))).contains(productType.toLowerCase())){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.PRODUCT_TYPE_ERROR));
            }
            else if(action.trim().isEmpty() || action.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_ACTION));
            }
            else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.ACTION_TYPES))).contains(action.toLowerCase())){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.ACTION_TYPE_ERROR));
            }
            else if(orderType.trim().isEmpty() || orderType.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_ORDER_TYPE));
            }
//            else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.ORDER_TYPES))).contains(orderType.toLowerCase())){
//                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.ORDER_TYPE_ERROR));
//            }
            else if(quantity.trim().isEmpty() || quantity.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_QUANTITY));
            }
            else if(validity.trim().isEmpty() || validity.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_VALIDITY));
            }
            else if(!(Arrays.asList(config.typeLists.get(Config.ListEnum.VALIDITY_TYPES))).contains(validity.toLowerCase())){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.VALIDITY_TYPE_ERROR));
            }
            else if(!(right.trim().isEmpty() || right.isEmpty()) && (!(Arrays.asList(config.typeLists.get(Config.ListEnum.RIGHT_TYPES))).contains(right.toLowerCase()))){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.RIGHT_TYPE_ERROR));
            }
            JSONObject body = new JSONObject();
            body.put("stock_code",stockCode);
            body.put("exchange_code",exchangeCode);
            body.put("product",productType);
            body.put("action",action);
            body.put("order_type",orderType);
            body.put("quantity",quantity);
            body.put("price",price);
            body.put("validity",validity);

            if(!(stoploss.trim().isEmpty() && stoploss.isEmpty())){
                body.put("stoploss",stoploss);
            }
            if(!(validityDate.trim().isEmpty() && validityDate.isEmpty())){
                body.put("validity_date",validityDate);
            }
            if(!(disclosedQuantity.trim().isEmpty() && disclosedQuantity.isEmpty())){
                body.put("disclosed_quantity",disclosedQuantity);
            }
            if(!(expiryDate.trim().isEmpty() && expiryDate.isEmpty())){
                body.put("expiry_date",expiryDate);
            }
            if(!(right.trim().isEmpty() && right.isEmpty())){
                body.put("right",right);
            }
            if(!(strikePrice.trim().isEmpty() && strikePrice.isEmpty())){
                body.put("strike_price",strikePrice);
            }
            if(!(userRemark.trim().isEmpty() && userRemark.isEmpty())){
                body.put("user_remark",userRemark);
            }
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.POST),
                    config.endPoints.get(Config.EndPointEnum.ORDER),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getOrderDetail(String exchangeCode, String orderId){
        try{
            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(orderId.trim().isEmpty() || orderId.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_ORDER_ID));
            }
            JSONObject body = new JSONObject();
            body.put("exchange_code",exchangeCode);
            body.put("order_id",orderId);
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.ORDER),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getOrderList(String exchangeCode, String fromDate, String toDate){
        try{
            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(fromDate.trim().isEmpty() || fromDate.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_FROM_DATE));
            }
            else if(toDate.trim().isEmpty() || toDate.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_TO_DATE));
            }
            JSONObject body = new JSONObject();
            body.put("exchange_code",exchangeCode);
            body.put("from_date",fromDate);
            body.put("to_date",toDate);
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.ORDER),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject cancelOrder(String exchangeCode, String orderId){
        try{
            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(orderId.trim().isEmpty() || orderId.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_ORDER_ID));
            }
            JSONObject body = new JSONObject();
            body.put("exchange_code",exchangeCode);
            body.put("order_id",orderId);
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.DELETE),
                    config.endPoints.get(Config.EndPointEnum.ORDER),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject modifyOrder(String orderId, String exchangeCode, String orderType, String stoploss, String quantity, String price, String validity, String disclosedQuantity, String validityDate){
        try{
            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(orderId.trim().isEmpty() || orderId.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_ORDER_ID));
            }
            else if(!(orderType.trim().isEmpty() || orderType.isEmpty()) && (!(Arrays.asList(config.typeLists.get(Config.ListEnum.ORDER_TYPES))).contains(orderType.toLowerCase()))){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.ORDER_TYPE_ERROR));
            }
            else if(!(validity.trim().isEmpty() || validity.isEmpty()) && (!(Arrays.asList(config.typeLists.get(Config.ListEnum.VALIDITY_TYPES))).contains(validity.toLowerCase()))){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.VALIDITY_TYPE_ERROR));
            }
            JSONObject body = new JSONObject();
            body.put("order_id",orderId);
            body.put("exchange_code",exchangeCode);
            if(!(orderType.trim().isEmpty() || orderType.isEmpty())){
                body.put("order_type",orderType);
            }
            if(!(stoploss.trim().isEmpty() || stoploss.isEmpty())){
                body.put("stoploss",stoploss);
            }
            if(!(quantity.trim().isEmpty() || quantity.isEmpty())){
                body.put("quantity",quantity);
            }
            if(!(price.trim().isEmpty() || price.isEmpty())){
                body.put("price",price);
            }
            if(!(validity.trim().isEmpty() || validity.isEmpty())){
                body.put("validity",validity);
            }
            if(!(disclosedQuantity.trim().isEmpty() || disclosedQuantity.isEmpty())){
                body.put("disclosed_quantity",disclosedQuantity);
            }
            if(!(validityDate.trim().isEmpty() || validityDate.isEmpty())){
                body.put("validity_date",validityDate);
            }
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.PUT),
                    config.endPoints.get(Config.EndPointEnum.ORDER),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getPortfolioHoldings(String exchangeCode, String fromDate, String toDate, String stockCode, String portfolioType){
        try{
            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            JSONObject body = new JSONObject();
            body.put("exchange_code",exchangeCode);
            if(!(fromDate.trim().isEmpty() || fromDate.isEmpty())){
                body.put("from_date",fromDate);
            }
            if(!(toDate.trim().isEmpty() || toDate.isEmpty())){
                body.put("to_date",toDate);
            }
            if(!(stockCode.trim().isEmpty() || stockCode.isEmpty())){
                body.put("stock_code",stockCode);
            }
            if(!(portfolioType.trim().isEmpty() || portfolioType.isEmpty())){
                body.put("portfolio_type",portfolioType);
            }
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.PORTFOLIO_HOLDING),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getPortfolioPositions(){
        try{
            JSONObject body = new JSONObject();
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.PORTFOLIO_POSITION),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    public JSONObject getQuotes(String stockCode, String exchangeCode, String expiryDate, String productType, String right, String strikePrice){
        try{
            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(stockCode.trim().isEmpty() || stockCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_STOCK_CODE));
            }
            else if(!(productType.trim().isEmpty() || productType.isEmpty()) && !(Arrays.asList(config.typeLists.get(Config.ListEnum.PRODUCT_TYPES))).contains(productType.toLowerCase())){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.PRODUCT_TYPE_ERROR));
            }
            else if(!(right.trim().isEmpty() || right.isEmpty()) && !(Arrays.asList(config.typeLists.get(Config.ListEnum.RIGHT_TYPES))).contains(right.toLowerCase())){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.RIGHT_TYPE_ERROR));
            }
            JSONObject body = new JSONObject();
            body.put("stock_code",stockCode);
            body.put("exchange_code",exchangeCode);
            if(!(expiryDate.trim().isEmpty() || expiryDate.isEmpty())){
                body.put("expiry_date",expiryDate);
            }
            if(!(productType.trim().isEmpty() || productType.isEmpty())){
                body.put("product_type",productType);
            }
            if(!(right.trim().isEmpty() || right.isEmpty())){
                body.put("right",right);
            }
            if(!(strikePrice.trim().isEmpty() || strikePrice.isEmpty())){
                body.put("strike_price",strikePrice);
            }
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.QUOTE),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    //public JSONObject marginCalculator()
    public JSONObject limitCalculator(String strikePrice, String productType, String expiryDate,String underlying,String exchangeCode,String orderFlow, String stoplossTrigger, String optionType,String sourceFlag,String limitRate,String orderReference,String availableQuantity,String marketType,String freshOrderLimit)
    {
        try
        {
            if(productType==null || productType.isEmpty() || productType.trim().isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_PRODUCT_TYPE_NFO));
            }
            else if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(stoplossTrigger.trim().isEmpty() || stoplossTrigger.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_STOP_LOSS_TRIGGER));
            }
            else if(optionType.trim().isEmpty() || optionType.isEmpty())
            {
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_OPTION_TYPE));
            }
            else if(strikePrice.trim().isEmpty() || strikePrice.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_STRIKE_PRICE));
            }
            JSONObject body = new JSONObject();
            body.put("strike_price",strikePrice);
            body.put("product_type",productType);
            body.put("expiry_date",expiryDate);
            body.put("underlying",underlying);
            body.put("exchange_code",exchangeCode);
            body.put("order_flow",orderFlow);
            body.put("stop_loss_trigger",stoplossTrigger);
            body.put("option_type",optionType);
            body.put("source_flag",sourceFlag);
            body.put("limit_rate",limitRate);
            body.put("order_reference",orderReference);
            body.put("available_quantity",availableQuantity);
            body.put("market_type", marketType);
            body.put("fresh_order_limit",freshOrderLimit);

            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.POST),
                    config.endPoints.get(Config.EndPointEnum.LIMIT_CALCULATOR),
                    body, headers
            );
            return new JSONObject(response);
        }
        catch(JSONException e)
        {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getOptionChainQuotes(String stockCode, String exchangeCode, String expiryDate, String productType, String right, String strikePrice){
        try{
            if(exchangeCode==null || exchangeCode.trim().isEmpty() || exchangeCode.isEmpty() || !exchangeCode.equalsIgnoreCase("nfo")){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.OPT_CHAIN_EXCH_CODE_ERROR));
            }
            else if(productType==null || productType.isEmpty() || productType.trim().isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_PRODUCT_TYPE_NFO));
            }
            else if(!productType.toLowerCase().equals("futures") && !productType.toLowerCase().equals("options")){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.PRODUCT_TYPE_ERROR));
            }
            else if(stockCode==null || stockCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(productType.toLowerCase().equals("options"))
            {
                if((expiryDate==null || expiryDate.isEmpty()) && (strikePrice==null || strikePrice.isEmpty()) && (right==null || right.isEmpty()))
                {
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_RIGHT_STRIKE_PRICE));
                }
                else if((expiryDate!=null || !expiryDate.isEmpty()) && (strikePrice==null || strikePrice.isEmpty()) && (right==null || right.isEmpty()))
                {
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_RIGHT_STRIKE_PRICE));
                }
                else if((expiryDate==null || expiryDate.isEmpty()) && (strikePrice!=null || !strikePrice.isEmpty()) && (right==null || right.isEmpty()))
                {
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_RIGHT_EXPIRY_DATE));
                }
                else if((expiryDate==null || expiryDate.isEmpty()) && (strikePrice==null || strikePrice.isEmpty()) && (right!=null || !right.isEmpty()))
                {
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXPIRY_DATE_STRIKE_PRICE));
                }
                else if((right!=null || !right.isEmpty()) && (!right.equalsIgnoreCase("put") && !right.equalsIgnoreCase("call") && !right.equalsIgnoreCase("others")))
                {
                    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.RIGHT_TYPE_ERROR));
                }
            }
            JSONObject body = new JSONObject();
            body.put("stock_code",stockCode);
            body.put("exchange_code",exchangeCode);
            if(!(expiryDate.trim().isEmpty() || expiryDate.isEmpty())){
                body.put("expiry_date",expiryDate);
            }
            if(!(productType.trim().isEmpty() || productType.isEmpty())){
                body.put("product_type",productType);
            }
            if(!(right.trim().isEmpty() || right.isEmpty())){
                body.put("right",right);
            }
            if(!(strikePrice.trim().isEmpty() || strikePrice.isEmpty())){
                body.put("strike_price",strikePrice);
            }
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.OPT_CHAIN),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject squareOff(String sourceFlag, String stockCode, String exchangeCode, String quantity, String price, String action, String orderType, String validity, String stoploss, String disclosedQuantity, String protectionPercentage, String settlementId, String marginAmount, String openQuantity, String coverQuantity, String productType, String expiryDate, String right, String strikePrice, String validityDate, String tradePassword, String aliasName){
        try{
            JSONObject body = new JSONObject();
            body.put("source_flag", (sourceFlag.isEmpty() || sourceFlag.trim().isEmpty()) ? "" : sourceFlag);
            body.put("stock_code", (stockCode.isEmpty() || stockCode.trim().isEmpty()) ? "" : stockCode);
            body.put("exchange_code", (exchangeCode.isEmpty() || exchangeCode.trim().isEmpty()) ? "" : exchangeCode);
            body.put("quantity", (quantity.isEmpty() || quantity.trim().isEmpty()) ? "" : quantity);
            body.put("price", (price.isEmpty() || price.trim().isEmpty()) ? "" : price);
            body.put("action", (action.isEmpty() || action.trim().isEmpty()) ? "" : action);
            body.put("order_type", (orderType.isEmpty() || orderType.trim().isEmpty()) ? "" : orderType);
            body.put("validity", (validity.isEmpty() || validity.trim().isEmpty()) ? "" : validity);
            body.put("stoploss_price", (stoploss.isEmpty() || stoploss.trim().isEmpty()) ? "" : stoploss);
            body.put("disclosed_quantity", (disclosedQuantity.isEmpty() || disclosedQuantity.trim().isEmpty()) ? "" : disclosedQuantity);
            body.put("protection_percentage", (protectionPercentage.isEmpty() || protectionPercentage.trim().isEmpty()) ? "" : protectionPercentage);
            body.put("settlement_id", (settlementId.isEmpty() || settlementId.trim().isEmpty()) ? "" : settlementId);
            body.put("margin_amount", (marginAmount.isEmpty() || marginAmount.trim().isEmpty()) ? "" : marginAmount);
            body.put("open_quantity", (openQuantity.isEmpty() || openQuantity.trim().isEmpty()) ? "" : openQuantity);
            body.put("cover_quantity", (coverQuantity.isEmpty() || coverQuantity.trim().isEmpty()) ? "" : coverQuantity);
            body.put("product_type", (productType.isEmpty() || productType.trim().isEmpty()) ? "" : productType);
            body.put("expiry_date", (expiryDate.isEmpty() || expiryDate.trim().isEmpty()) ? "" : expiryDate);
            body.put("right", (right.isEmpty() || right.trim().isEmpty()) ? "" : right);
            body.put("strike_price", (strikePrice.isEmpty() || strikePrice.trim().isEmpty()) ? "" : strikePrice);
            body.put("validity_date", (validityDate.isEmpty() || validityDate.trim().isEmpty()) ? "" : validityDate);
            body.put("alias_name", (aliasName.isEmpty() || aliasName.trim().isEmpty()) ? "" : aliasName);
            body.put("trade_password", (tradePassword.isEmpty() || tradePassword.trim().isEmpty()) ? "" : tradePassword);
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.POST),
                    config.endPoints.get(Config.EndPointEnum.SQUARE_OFF),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getTradeList(String fromDate, String toDate, String exchangeCode, String productType, String action, String stockCode){
        try{
            JSONObject body = new JSONObject();
            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(!(productType.trim().isEmpty() || productType.isEmpty()) && !(Arrays.asList(config.typeLists.get(Config.ListEnum.PRODUCT_TYPES))).contains(productType.toLowerCase())){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.PRODUCT_TYPE_ERROR));
            }
            else if(!(action.trim().isEmpty() || action.isEmpty()) && !(Arrays.asList(config.typeLists.get(Config.ListEnum.ACTION_TYPES))).contains(action.toLowerCase())){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.ACTION_TYPE_ERROR));
            }

            body.put("exchange_code", exchangeCode);
            if(!(fromDate.trim().isEmpty() || fromDate.isEmpty())){
                body.put("from_date",fromDate);
            }
            if(!(toDate.trim().isEmpty() || toDate.isEmpty())){
                body.put("to_date",toDate);
            }
            if(!(productType.trim().isEmpty() || productType.isEmpty())){
                body.put("product_type",productType);
                body.put("product_type",productType);
            }
            if(!(action.trim().isEmpty() || action.isEmpty())){
                body.put("action",action);
            }
            if(!(stockCode.trim().isEmpty() || stockCode.isEmpty())){
                body.put("stock_code",stockCode);
            }
            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.TRADE),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getTradeDetail(String exchangeCode, String orderId){
        try{
            JSONObject body = new JSONObject();
            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }
            else if(orderId.trim().isEmpty() || orderId.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_ORDER_ID));
            }
            JSONArray headers = generateHeaders(body);
            body.put("exchange_code", exchangeCode);
            body.put("order_id", orderId);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.TRADE),
                    body, headers
            );
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject previewOrder(String stockCode,String exchangeCode,String productType,String orderType,String price,String action,String quantity,String expiryDate,String right,String strikePrice,String specialFlag,String stoploss, String orderRateFresh)
    {
        try
        {

            if(exchangeCode.trim().isEmpty() || exchangeCode.isEmpty()){
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }

            else if(stockCode==null || stockCode.isEmpty())
            {
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_EXCHANGE_CODE));
            }

            else if(action.trim().isEmpty() || action.isEmpty())
            {
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_ACTION));
            }

            else if(productType.trim().isEmpty() || productType.isEmpty())
            {
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_PRODUCT_TYPE));
            }

            else if(orderType.trim().isEmpty() || orderType.isEmpty())
            {
                return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_ORDER_TYPE));
            }

            //else if(right.trim().isEmpty() || right.isEmpty())
            //{
            //    return this.validationResponse("",500,config.responseMessage.get(Config.ResponseEnum.BLANK_RIGHT_TYPE));
            //}

            JSONObject body = new JSONObject();

            body.put("stock_code",stockCode);
            body.put("exchange_code",exchangeCode);
            body.put("product",productType);
            body.put("order_type",orderType);
            body.put("price",price);
            body.put("action",action);
            body.put("quantity",quantity);
            body.put("expiry_date",expiryDate);
            body.put("right",right);
            body.put("strike_price",strikePrice);
            body.put("specialflag",specialFlag);
            body.put("stoploss",stoploss);
            body.put("order_rate_fresh",orderRateFresh);


            JSONArray headers = generateHeaders(body);
            String response = makeRequest(
                    config.apiMethods.get(Config.APIMethodEnum.GET),
                    config.endPoints.get(Config.EndPointEnum.PREVIEW_ORDER),
                    body, headers
            );
            return new JSONObject(response);

        }
        catch(JSONException e)
        {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getNames(String exchange, String stockcode) throws Exception
    {
        try
        {
            URL url;
            Map<String, List<String>> exchangeCodeAskey = new HashMap<>(); //it contain values as token,isec code, company.
            Map<String,List<String>> isceCodeAskey = new HashMap<>(); //it contain values as token,exchange code, company.
            exchange = exchange.toLowerCase();
            stockcode = stockcode.toUpperCase();
            switch(exchange)
            {
                case "nse":
                    url = new URL(config.isecNseCodeMapFile.get("nse"));
                    break;
                case "bse":
                    url = new URL(config.isecNseCodeMapFile.get("bse"));
                    break;
                case "cdnse":
                    url = new URL(config.isecNseCodeMapFile.get("cdnse"));
                    break;
                case "fonse":
                    url = new URL(config.isecNseCodeMapFile.get("fonse"));
                    break;
                default:
                    url = new URL(config.isecNseCodeMapFile.get("nse"));
                    break;
            }


            InputStream is = url.openStream();
            Scanner s = new Scanner(is);
            s.nextLine();
            String data[];
            while(s.hasNext())
            {
                String  row = s.nextLine();
                data = row.split(",");
                List<String> valueForExchange = new ArrayList<>();
                valueForExchange.add(data[0]);
                valueForExchange.add(data[1]);
                valueForExchange.add(data[3]);

                String exchangetoken = data[60].replaceAll("^\"|\"$", "");

                if(exchangetoken.equals(stockcode)){
                    exchangeCodeAskey.put(exchangetoken, valueForExchange);
                }

                List<String> valueForIsec = new ArrayList<>();
                valueForIsec.add(data[0]);
                valueForIsec.add(data[60]);
                valueForIsec.add(data[3]);
                String isecCode = data[1].replaceAll("^\"|\"$", "");

                if(isecCode.equals(stockcode)){
                    isceCodeAskey.put(isecCode, valueForIsec);
                }

            }
            Map<String,String> dictionary = new HashMap<>();
            List<String> list = exchangeCodeAskey.get(stockcode);
            String result ="";

            if(list!=null)
            {
                result = "{"+"\n"+ "exchange_stock_Code: "+stockcode+"\n"+
                        "token: "+list.get(0)+"\n"+
                        "isec stock code: "+list.get(1)+"\n"+
                        "company name: "+list.get(2)+"\n"+
                        "exchange_code: "+ exchange+"\n"+
                        "}";
                return(new JSONObject(result));
            }

            list = isceCodeAskey.get(stockcode);
            if(list!=null)
            {
                result = "{"+"\n"+"exchange_stock_Code:"+ list.get(1)+"\n"
                        +"token:"+list.get(0)+"\n"+
                        "isec stock code:"+stockcode+"\n"+
                        "company name:"+ list.get(2)+"\n"+
                        "exchange_code:"+exchange+"\n" +
                        "}";
                return(new JSONObject(result));
            }
            return(new JSONObject("{\n"+"status:404\n"+"}"));

        }
        catch(Exception e)
        {
            return(new JSONObject("{\n"+"status:404\n"+"}"));
        }
    }
}