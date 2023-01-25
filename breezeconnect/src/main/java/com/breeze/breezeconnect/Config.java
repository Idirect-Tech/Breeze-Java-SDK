package com.breeze.breezeconnect;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class Config {

    public enum APIMethodEnum{
        POST, GET, PUT, DELETE
    }

    public enum EndPointEnum{
        CUST_DETAILS, DEMAT_HOLDING, FUND, HIST_CHART, MARGIN,
        ORDER, PORTFOLIO_HOLDING, PORTFOLIO_POSITION, QUOTE,
        TRADE, OPT_CHAIN, SQUARE_OFF,PREVIEW_ORDER
    }

    public enum UrlEnum{
        API_URL, BREEZE_NEW_URL, LIVE_FEEDS_URL, LIVE_STREAM_URL,
        LIVE_OHLC_STREAM_URL, SECURITY_MASTER_URL, STOCK_SCRIPT_CSV_URL
    }

    public enum ResponseEnum{

        // Empty Details Errors
        BLANK_EXCHANGE_CODE, BLANK_STOCK_CODE, BLANK_PRODUCT_TYPE ,BLANK_PRODUCT_TYPE_NFO, BLANK_ACTION,
        BLANK_PRODUCT_TYPE_HIST_V2, BLANK_ORDER_TYPE, BLANK_QUANTITY, BLANK_VALIDITY, BLANK_ORDER_ID,
        BLANK_FROM_DATE, BLANK_TO_DATE, BLANK_TRANSACTION_TYPE, BLANK_AMOUNT, BLANK_SEGMENT,BLANK_INTERVAL,
        BLANK_STRIKE_PRICE, BLANK_EXPIRY_DATE, BLANK_RIGHT_STRIKE_PRICE,BLANK_RIGHT_EXPIRY_DATE, BLANK_EXPIRY_DATE_STRIKE_PRICE,
        BLANK_RIGHT_TYPE,

        //Validation Errors
        EXCHANGE_CODE_ERROR ,EXCHANGE_CODE_HIST_V2_ERROR, PRODUCT_TYPE_ERROR , PRODUCT_TYPE_ERROR_NFO, PRODUCT_TYPE_ERROR_HIST_V2,
        ACTION_TYPE_ERROR, ORDER_TYPE_ERROR, VALIDITY_TYPE_ERROR, RIGHT_TYPE_ERROR, TRANSACTION_TYPE_ERROR, ZERO_AMOUNT_ERROR,
        INTERVAL_TYPE_ERROR,INTERVAL_TYPE_ERROR_HIST_V2,API_SESSION_ERROR, OPT_CHAIN_EXCH_CODE_ERROR, NFO_FIELDS_MISSING_ERROR,

        //Socket Connectivity Response
        RATE_REFRESH_NOT_CONNECTED, RATE_REFRESH_DISCONNECTED, ORDER_REFRESH_NOT_CONNECTED, ORDER_REFRESH_DISCONNECTED,
        ORDER_NOTIFICATION_SUBSCRIBED, OHLCV_STREAM_NOT_CONNECTED, OHLCV_STREAM_DISCONNECTED, ORDER_FLAG_NOT_SET,

        //Stock Subscription Message
        STOCK_SUBSCRIBE_MESSAGE, STOCK_UNSUBSCRIBE_MESSAGE
    }

    public enum ExceptionEnum{
        //Authentication Error
        AUTHENICATION_EXCEPTION,

        //Subscribe Exception
        QUOTE_DEPTH_EXCEPTION, EXCHANGE_CODE_EXCEPTION, STOCK_CODE_EXCEPTION, EXPIRY_DATE_EXCEPTION, PRODUCT_TYPE_EXCEPTION,
        STRIKE_PRICE_EXCEPTION, RIGHT_EXCEPTION, STOCK_INVALID_EXCEPTION, WRONG_EXCHANGE_CODE_EXCEPTION, STOCK_NOT_EXIST_EXCEPTION,
        ISEC_NSE_STOCK_MAP_EXCEPTION, STREAM_OHLC_INTERVAL_ERROR,

        //API Call Exception
        API_REQUEST_EXCEPTION, INVALID_REQUEST_EXCEPTION
    }

    public enum ListEnum{
        INTERVAL_TYPES, INTERVAL_TYPES_HIST_V2, INTERVAL_TYPES_STREAM_OHLC, PRODUCT_TYPES, PRODUCT_TYPES_HIST, PRODUCT_TYPES_HIST_V2,
        RIGHT_TYPES, ACTION_TYPES, ORDER_TYPES, VALIDITY_TYPES, TRANSACTION_TYPES, EXCHANGE_CODES_HIST, EXCHANGE_CODES_HIST_V2
    }

    public EnumMap<EndPointEnum, String> endPoints;
    public EnumMap<UrlEnum,String> urls;
    public EnumMap<ResponseEnum,String> responseMessage;
    public EnumMap<ExceptionEnum,String> exceptionMessage;
    public EnumMap<APIMethodEnum,String> apiMethods;
    public Map<String,Map<String,String>> tuxToUserValue;
    public EnumMap<ListEnum, String[]> typeLists;
    public Map<String,String> isecNseCodeMapFile;
    public Map<String,String> feedIntervalMap;
    public Map<String,String> channelIntervalMap;

    public Config(){

        apiMethods = new EnumMap<APIMethodEnum, String>(APIMethodEnum.class){{
           put(APIMethodEnum.GET,"GET");
           put(APIMethodEnum.POST,"POST");
           put(APIMethodEnum.PUT,"PUT");
           put(APIMethodEnum.DELETE,"DELETE");
        }};

        urls = new EnumMap<UrlEnum, String>(UrlEnum.class){{
           put(UrlEnum.API_URL,"https://api.icicidirect.com/breezeapi/api/v1/");
           put(UrlEnum.BREEZE_NEW_URL,"https://breezeapi.icicidirect.com/api/v2/");
           put(UrlEnum.LIVE_FEEDS_URL,"https://livefeeds.icicidirect.com");
           put(UrlEnum.LIVE_STREAM_URL,"https://livestream.icicidirect.com");
           put(UrlEnum.LIVE_OHLC_STREAM_URL,"https://breezeapi.icicidirect.com");
           put(UrlEnum.SECURITY_MASTER_URL,"https://directlink.icicidirect.com/NewSecurityMaster/SecurityMaster.zip");
           put(UrlEnum.STOCK_SCRIPT_CSV_URL,"https://traderweb.icicidirect.com/Content/File/txtFile/ScripFile/StockScriptNew.csv");
        }};

        endPoints = new EnumMap<EndPointEnum, String>(EndPointEnum.class){{
            put(EndPointEnum.CUST_DETAILS,"customerdetails");
            put(EndPointEnum.DEMAT_HOLDING,"dematholdings");
            put(EndPointEnum.FUND,"funds");
            put(EndPointEnum.MARGIN,"margin");
            put(EndPointEnum.ORDER,"order");
            put(EndPointEnum.PORTFOLIO_HOLDING,"portfolioholdings");
            put(EndPointEnum.PORTFOLIO_POSITION,"portfoliopositions");
            put(EndPointEnum.QUOTE,"quotes");
            put(EndPointEnum.TRADE,"trades");
            put(EndPointEnum.OPT_CHAIN,"optionchain");
            put(EndPointEnum.SQUARE_OFF,"squareoff");
            put(EndPointEnum.HIST_CHART,"historicalcharts");
            put(EndPointEnum.PREVIEW_ORDER,"preview_order");
        }
        };
        createTuxMap();
        createResponseMap();
        createExceptionMap();
        createTypeListMap();

        isecNseCodeMapFile = new HashMap<>(){{
            put("nse","https://traderweb.icicidirect.com/Content/File/txtFile/ScripFile/NSEScripMaster.txt");
            put("bse","https://traderweb.icicidirect.com/Content/File/txtFile/ScripFile/BSEScripMaster.txt");
            put("cdnse","https://traderweb.icicidirect.com/Content/File/txtFile/ScripFile/CDNSEScripMaster.txt");
            put("fonse","https://traderweb.icicidirect.com/Content/File/txtFile/ScripFile/FONSEScripMaster.txt");
        }};

        feedIntervalMap = new HashMap<>(){{
            put("1MIN","1minute");
            put("5MIN","5minute");
            put("30MIN","30minute");
            put("1SEC","1second");
        }};

        channelIntervalMap = new HashMap<>(){{
            put("1minute","1MIN");
            put("5minute","5MIN");
            put("30minute","30MIN");
            put("1second","1SEC");
        }};
    }

    public void createTuxMap(){
        tuxToUserValue = new HashMap<>(){{
           put("orderFlow",new HashMap<>(){{
               put("B","Buy");
               put("S","Sell");
               put("N","NA");
           }});
            put("limitMarketFlag",new HashMap<>(){{
                put("L","Limit");
                put("I","IoC");
                put("V","VTC");
            }});
            put("productType",new HashMap<>(){{
                put("F","Futures");
                put("O","Options");
                put("P","FuturePlus");
                put("U","FuturePlus_sltp");
                put("I","OptionPlus");
                put("C","Cash");
                put("Y","eATM");
                put("B","BTST");
                put("M","Margin");
                put("T","MarginPlus");
            }});
            put("orderStatus",new HashMap<>(){{
                put("A","All");
                put("R","Requested");
                put("Q","Queued");
                put("O","Ordered");
                put("P","Partially Executed");
                put("E","Executed");
                put("J", "Rejected");
                put("X", "Expired");
                put("B", "Partially Executed And Expired");
                put("D", "Partially Executed And Cancelled");
                put("F", "Freezed");
                put("C", "Cancelled");
            }});
            put("optionType",new HashMap<>(){{
                put("C","Call");
                put("P","Put");
                put("*","Others");
            }});
        }};
    }

    public void createResponseMap(){
        responseMessage = new EnumMap<ResponseEnum,String>(ResponseEnum.class){{

            //Empty Details Errors
            put(ResponseEnum.BLANK_EXCHANGE_CODE,"Exchange-Code cannot be empty");
            put(ResponseEnum.BLANK_STOCK_CODE,"Stock-Code cannot be empty");
            put(ResponseEnum.BLANK_PRODUCT_TYPE,"Product cannot be empty");
            put(ResponseEnum.BLANK_PRODUCT_TYPE_NFO,"Product-type cannot be empty for Exchange-Code 'nfo'");
            put(ResponseEnum.BLANK_PRODUCT_TYPE_HIST_V2,"Product-type cannot be empty for Exchange-Code 'nfo','ndx' or 'mcx'");
            put(ResponseEnum.BLANK_ACTION,"Action cannot be empty");
            put(ResponseEnum.BLANK_ORDER_TYPE,"Order-type cannot be empty");
            put(ResponseEnum.BLANK_QUANTITY,"Quantity cannot be empty");
            put(ResponseEnum.BLANK_VALIDITY,"Validity cannot be empty");
            put(ResponseEnum.BLANK_ORDER_ID,"Order-Id cannot be empty");
            put(ResponseEnum.BLANK_FROM_DATE,"From-Date cannot be empty");
            put(ResponseEnum.BLANK_TO_DATE,"To-Date cannot be empty");
            put(ResponseEnum.BLANK_TRANSACTION_TYPE,"Transaction-Type cannot be empty");
            put(ResponseEnum.BLANK_AMOUNT,"Amount cannot be empty");
            put(ResponseEnum.BLANK_SEGMENT,"Segment cannot be empty");
            put(ResponseEnum.BLANK_INTERVAL,"Interval cannot be empty");
            put(ResponseEnum.BLANK_RIGHT_TYPE,"Right cannot be empty for Product-Type");
            put(ResponseEnum.BLANK_STRIKE_PRICE,"Strike-Price cannot be empty for Product-Type 'options'");
            put(ResponseEnum.BLANK_EXPIRY_DATE,"Expiry-Date cannot be empty for exchange-code 'nfo'");
            put(ResponseEnum.BLANK_RIGHT_STRIKE_PRICE,"Either Right or Strike-Price cannot be empty.");
            put(ResponseEnum.BLANK_RIGHT_EXPIRY_DATE,"Either Expiry-Date or Right cannot be empty.");
            put(ResponseEnum.BLANK_EXPIRY_DATE_STRIKE_PRICE,"Either Expiry-Date or Strike-Price cannot be empty.");

            //Validation Errors
            put(ResponseEnum.EXCHANGE_CODE_ERROR,"Exchange-Code should be either 'nse', or 'nfo'");
            put(ResponseEnum.EXCHANGE_CODE_HIST_V2_ERROR,"Exchange-Code should be either 'nse', 'bse' ,'nfo', 'ndx' or 'mcx'");
            put(ResponseEnum.PRODUCT_TYPE_ERROR,"Product should be either 'futures', 'options', 'futureplus', 'optionplus', 'cash', 'eatm', or 'margin'");
            put(ResponseEnum.PRODUCT_TYPE_ERROR_NFO,"Product-type should be either 'futures', 'options', 'futureplus', or 'optionplus' for Exchange-Code 'NFO'");
            put(ResponseEnum.PRODUCT_TYPE_ERROR_HIST_V2,"Product-type should be either 'futures', 'options' for Exchange-Code 'NFO','NDX' or 'MCX'");
            put(ResponseEnum.ACTION_TYPE_ERROR,"Action should be either 'buy', or 'sell'");
            put(ResponseEnum.ORDER_TYPE_ERROR,"Order-type should be either 'limit', 'market', or 'stoploss'");
            put(ResponseEnum.VALIDITY_TYPE_ERROR,"Validity should be either 'day', 'ioc', or 'vtc'");
            put(ResponseEnum.RIGHT_TYPE_ERROR,"Right should be either 'call', 'put', or 'others'");
            put(ResponseEnum.TRANSACTION_TYPE_ERROR,"Transaction-Type should be either 'debit' or 'credit'");
            put(ResponseEnum.ZERO_AMOUNT_ERROR,"Amount should be more than 0");
            put(ResponseEnum.INTERVAL_TYPE_ERROR,"Interval should be either '1minute', '5minute', '30minute', or '1day'");
            put(ResponseEnum.INTERVAL_TYPE_ERROR_HIST_V2,"Interval should be either '1second','1minute', '5minute', '30minute', or '1day'");
            put(ResponseEnum.API_SESSION_ERROR,"API Session cannot be empty");
            put(ResponseEnum.OPT_CHAIN_EXCH_CODE_ERROR,"Exchange code should be nfo");
            put(ResponseEnum.NFO_FIELDS_MISSING_ERROR,"Atleast two inputs are required out of Expiry-Date, Right & Strike-Price. All three cannot be empty'.");

            //Socket Connectivity Response
            put(ResponseEnum.RATE_REFRESH_NOT_CONNECTED,"socket server is not connected to rate refresh.");
            put(ResponseEnum.RATE_REFRESH_DISCONNECTED,"socket server for rate refresh  has been disconnected.");
            put(ResponseEnum.ORDER_REFRESH_NOT_CONNECTED,"socket server is not connected to order refresh.");
            put(ResponseEnum.ORDER_REFRESH_DISCONNECTED,"socket server for order streaming has been disconnected.");
            put(ResponseEnum.ORDER_NOTIFICATION_SUBSCRIBED,"Order Notification subscribed successfully");
            put(ResponseEnum.OHLCV_STREAM_NOT_CONNECTED,"socket server is not connected to OHLCV Stream.");
            put(ResponseEnum.OHLCV_STREAM_DISCONNECTED,"socket server for OHLCV Streaming has been disconnected.");
            put(ResponseEnum.ORDER_FLAG_NOT_SET,"getOrderNotification should be %s");

            //Stock Subscription Message
            put(ResponseEnum.STOCK_SUBSCRIBE_MESSAGE,"Stock %s subscribed successfully");
            put(ResponseEnum.STOCK_UNSUBSCRIBE_MESSAGE,"Stock %s unsubscribed successfully");
        }
        };
    }

    public void createExceptionMap(){
        exceptionMessage = new EnumMap<ExceptionEnum, String>(ExceptionEnum.class){{
            //Authentication Error
            put(ExceptionEnum.AUTHENICATION_EXCEPTION,"Could not authenticate credentials. Please check token and keys");

            //Subscribe Exception
            put(ExceptionEnum.QUOTE_DEPTH_EXCEPTION,"Either getExchangeQuotes must be true or getMarketDepth must be true");
            put(ExceptionEnum.EXCHANGE_CODE_EXCEPTION,"Exchange Code allowed are 'BSE', 'NSE', 'NDX', 'MCX' or 'NFO'.");
            put(ExceptionEnum.STOCK_CODE_EXCEPTION,"Stock-Code cannot be empty.");
            put(ExceptionEnum.EXPIRY_DATE_EXCEPTION,"Expiry-Date cannot be empty for given Exchange-Code.");
            put(ExceptionEnum.PRODUCT_TYPE_EXCEPTION,"Product-Type should either be Futures or Options for given Exchange-Code.");
            put(ExceptionEnum.STRIKE_PRICE_EXCEPTION,"Strike Price cannot be empty for Product-Type 'Options'.");
            put(ExceptionEnum.RIGHT_EXCEPTION,"Rights should either be Put or Call for Product-Type 'Options'.");
            put(ExceptionEnum.STOCK_INVALID_EXCEPTION,"Stock-Code not found.");
            put(ExceptionEnum.WRONG_EXCHANGE_CODE_EXCEPTION,"Stock-Token cannot be found due to wrong exchange-code.");
            put(ExceptionEnum.STOCK_NOT_EXIST_EXCEPTION,"Stock-Data does not exist in exchange-code %s for Stock-Token %s.");
            put(ExceptionEnum.ISEC_NSE_STOCK_MAP_EXCEPTION,"Result Not Found");
            put(ExceptionEnum.STREAM_OHLC_INTERVAL_ERROR,"Interval should be either '1second','1minute', '5minute', '30minute'");

            //API Call Exception
            put(ExceptionEnum.API_REQUEST_EXCEPTION,"Error while trying to make request %s %s");
            put(ExceptionEnum.INVALID_REQUEST_EXCEPTION,"Invalid Request Method - Must be GET, POST, PUT or DELETE");
        }};
    }

    public void createTypeListMap(){
        typeLists = new EnumMap<ListEnum, String[]>(ListEnum.class){{
           put(ListEnum.INTERVAL_TYPES,new String[]{"1minute", "5minute", "30minute", "1day"});
           put(ListEnum.INTERVAL_TYPES_HIST_V2,new String[]{"1second","1minute", "5minute", "30minute", "1day"});
           put(ListEnum.INTERVAL_TYPES_STREAM_OHLC,new String[]{"1second","1minute", "5minute", "30minute"});
           put(ListEnum.PRODUCT_TYPES, new String[]{"futures", "options", "futureplus", "optionplus", "cash", "eatm", "margin"});
           put(ListEnum.PRODUCT_TYPES_HIST, new String[]{"futures", "options", "futureplus", "optionplus"});
           put(ListEnum.PRODUCT_TYPES_HIST_V2, new String[]{"futures", "options","cash"});
           put(ListEnum.RIGHT_TYPES,new String[]{"call", "put", "others"});
           put(ListEnum.ACTION_TYPES,new String[]{"buy", "sell"});
           put(ListEnum.VALIDITY_TYPES,new String[]{"day", "ioc", "vtc"});
           put(ListEnum.TRANSACTION_TYPES,new String[]{"debit", "credit"});
           put(ListEnum.EXCHANGE_CODES_HIST,new String[]{"nse", "nfo"});
           put(ListEnum.EXCHANGE_CODES_HIST_V2,new String[]{"nse","bse","nfo","ndx","mcx"});
        }};
    }
}
