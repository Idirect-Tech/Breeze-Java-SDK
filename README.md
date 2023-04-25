# Table Of Content

<ul>
 <li><a href="#client">Breeze API Java Client</a></li>
 <li><a href="#docslink">API Documentation</a></li>
 <li><a href="#virtualenv">Set Up Virtual Environment</a></li>
 <li><a href="#clientinstall">Installing Client</a></li>
 <li><a href="#apiusage">API Usage</a></li>
 <li><a href="#websocket">Websocket Usage</a></li>
 <li><a href="#index_title">List Of Other SDK methods</a></li>
</ul>

<h4>Usage</h4>

Download the jar file of the project from the following link <a href = "https://github.com/Idirect-Tech/Breeze-Java-SDK/blob/develop/jarbuild/breezeConnect-0.0.4-SNAPSHOT.jar" download>breezeconnect jar file </a>and include it in the build path of your  project.

<h4 id="client">Breeze API Java Client</h4>

breezeapi@icicisecurities.com

The official Java client library for the ICICI Securities trading APIs. BreezeConnect is a set of REST-like APIs that allows one to build a complete investment and trading platform. Following are some notable features of Breeze APIs:

1. Execute orders in real time
2. Manage Portfolio
3. Access to 10 years of historical market data including 1 sec OHLCV
4. Streaming live OHLC (websockets)
5. Option Chain API


<h4 id="docslink">API Documentation</h4>

<div class="sticky" >
<ul>
 <li><a href="https://api.icicidirect.com/breezeapi/documents/index.html">Breeze HTTP API Documentation</a></li>
</ul>
</div>

<h4 id="apiusage"> API Usage</h4>

```java

// Initialize SDK
BreezeConnect breezeConnect = new BreezeConnect("your_api_key");

// Obtain your session key from https://api.icicidirect.com/apiuser/login?api_key=YOUR_API_KEY
// Incase your api-key has special characters(like +,=,!) then encode the api key before using in the url as shown below.
System.out.println("https://api.icicidirect.com/apiuser/login?api_key="+URLEncoder.encode("your_api_key", StandardCharsets.UTF_8));

// Generate Session
breezeConnect.generateSession("your_secret_key","your_api_session");

```
<br>

<h4 id ="websocket"> Websocket Usage</h4>

```java
// Initialize SDK
BreezeConnect breezeConnect = new BreezeConnect("your_api_key");

// Obtain your session key from https://api.icicidirect.com/apiuser/login?api_key=YOUR_API_KEY
// Incase your api-key has special characters(like +,=,!) then encode the api key before using in the url as shown below.
System.out.println("https://api.icicidirect.com/apiuser/login?api_key="+URLEncoder.encode("your_api_key", StandardCharsets.UTF_8));

// Generate Session
breezeConnect.generateSession("your_secret_key","your_api_session");

// Connect to websocket(it will connect to rate refresh server)
breezeConnect.connectTicker();

// Callback to receive ticks.
breezeConnect.registerOnTickEventListener(new OnTickEventListener() {
    @Override
    public void onTickEvent(Object tick) {
        System.out.println(tick.toString());
    }
});

// subscribe stocks feeds

breezeConnect.subscribeFeeds("NFO","ZEEENT","options","31-Mar-2022","350","Call", true, false);

// subscribe stocks feeds by stock-token
breezeConnect.subscribeFeeds("1.1!500780");

// unsubscribe stocks feeds
breezeConnect.unsubscribeFeeds("NFO","ZEEENT","options","31-Mar-2022","350","Call", true, false);

// unsubscribe stocks feeds by stock-token
breezeConnect.unsubscribeFeeds("1.1!500780");

// subscribe to Real Time Streaming OHLCV Data of stocks
breezeConnect.subscribeFeeds("NFO", "ZEEENT", "options", "31-Mar-2022","350", "Call", "1minute");

// subscribe to Real Time Streaming OHLCV Data of stocks by stock-token
breezeConnect.subscribeFeeds("1.1!500780","1second");

// unsubscribe to Real Time Streaming OHLCV Data of stocks
breezeConnect.unsubscribeFeeds("NFO", "ZEEENT", "options", "31-Mar-2022", "350", right="Call", interval="1minute");

// unsubscribe to Real Time Streaming OHLCV Data of stocks by stock-token
breezeConnect.unsubscribeFeeds("1.1!500780","1second");

// subscribe order notification feeds(it will connect to order streaming server)
breezeConnect.subscribeFeeds(true)

// unsubscribe order notification feeds(also it will disconnect the order streaming server)
breezeConnect.unsubscribeFeeds(get_order_notification=true)

```
<br>

---

**NOTE**

Examples for stock_token are "4.1!38071" or "1.1!500780".

Template for stock_token : X.Y!<token>
X : exchange code
Y : Market Level data
Token : ISEC stock code

Value of X can be :
1 for BSE,
4 for NSE,
13 for NDX,
6 for MCX,
4 for NFO,

Value of Y can be :
1 for Level 1 data,
4 for Level 2 data

Token number can be obtained via get_names() function or downloading master security file via 
https://api.icicidirect.com/breezeapi/documents/index.html#instruments


exchange_code must be 'BSE', 'NSE', 'NDX', 'MCX' or 'NFO'.

stock_code should not be an empty string. Examples for stock_code are "WIPRO" or "ZEEENT".

product_type can be either 'Futures', 'Options' or an empty string. 
Product_type can not be an empty string for exchange_code 'NDX', 'MCX' and 'NFO'. 

strike_date can be in DD-MMM-YYYY(Ex.: 01-Jan-2022) or an empty string. 
strike_date can not be an empty string for exchange_code 'NDX', 'MCX' and 'NFO'.

strike_price can be float-value in string or an empty string. 
strike_price can not be an empty string for product_type 'Options'.

right can be either 'Put', 'Call' or an empty string. right can not be an empty string for product_type 'Options'.

Either get_exchange_quotes must be True or get_market_depth must be True. 

Both get_exchange_quotes and get_market_depth can be True, But both must not be False.

For Streaming OHLCV, interval must not be empty and must be equal to either of the following "1second","1minute", "5minute", "30minute"

---

<h4> List of other SDK Methods:</h4>

<h5 id="index_title" >Index</h5>

<div class="sticky" id="index">
<ul>
 <li><a href="#customer_detail">getCustomerDetails</a></li>
 <li><a href="#demat_holding">getDematHoldings</a></li>
 <li><a href="#get_funds">getFunds</a></li>
 <li><a href="#set_funds">setFunds</a></li>
 <li><a href="#historical_data1">getHistoricalData</a></li>
 <li><a href="#historical_data_v21">getHistoricalDatav2</a></li>
 <li><a href="#add_margin">addMargin</a></li>
 <li><a href="#get_margin">getMargin</a></li>
 <li><a href="#place_order">placeOrder</a></li>
 <li><a href="#order_detail">orderDetail</a></li>
 <li><a href="#order_list">orderList</a></li>
 <li><a href="#cancel_order">cancelOrder</a></li>
 <li><a href="#modify_order">modifyOrder</a></li>
 <li><a href="#portfolio_holding">getPortfolioHolding</a></li>
 <li><a href="#portfolio_position">getPortfolioPosition</a></li>
 <li><a href="#get_quotes">getQuotes</a></li>
 <li><a href="#get_option_chain">getOptionChainQuotes</a></li>
 <li><a href="#square_off1">squareOff</a></li>
 <li><a href="#modify_order">modifyOrder</a></li>
 <li><a href="#trade_list">getTradeList</a></li>
 <li><a href="#trade_detail">getTradeDetail</a></li>
 <li><a href="#get_names"> getNames </a></li>
 <li><a href="#preview_order"> previewOrder </a></li>
</ul>
</div>


<h4 id="customer_detail" > Get Customer details by api-session value.</h4>


```java
breezeConnect.getCustomerDetails("your_api_session");
```

<br>
<a href="#index">Back to Index</a>
<hr>


<h4 id="demat_holding"> Get Demat Holding details of your account.</h4>

```java

breezeConnect.getDematHoldings();

```
<br>
<a href="#index">Back to Index</a>
<hr>


<h4 id="get_funds"> Get Funds details of your account.</h4>


```java

breezeConnect.getFunds();

```

<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="set_funds"> Set Funds of your account</h4>


```java
breezeConnect.setFunds("debit","200","Equity");
```

<p> Note: Set Funds of your account by transaction-type as "Credit" or "Debit" with amount in numeric string as rupees and segment-type as "Equity" or "FNO".</p>
<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="historical_data1">Get Historical Data for Futures</h4>


```java
breezeConnect.getHistoricalData(
    "1minute","2022-08-15T07:00:00.000Z","2022-08-17T07:00:00.000Z",
    "ICIBAN","NFO","futures","2022-08-25T07:00:00.000Z","others","0"
);
```

<a href="#index">Back to Index</a>

<h4 id="historical_data2">Get Historical Data for Equity</h4>


```java
breezeConnect.getHistoricalData(
    "1minute","2022-08-15T07:00:00.000Z","2022-08-17T07:00:00.000Z",
    "ITC","NSE","cash"
);
```

<a href="#index">Back to Index</a>


<h4 id="historical_data3">Get Historical Data for Options</h4>


```java

breezeConnect.getHistoricalData(
    "1minute","2022-08-15T07:00:00.000Z","2022-08-17T07:00:00.000Z",
    "CNXBAN","NFO","options","2022-09-29T07:00:00.000Z","call","38000"
);
```


<p> Note : Get Historical Data for specific stock-code by mentioned interval either as "1minute", "5minute", "30minute" or as "1day"</p>
<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="historical_data_v21">Get Historical Data (version 2) for Futures</h4>


```java
breezeConnect.getHistoricalDatav2(
    "1minute","2022-08-15T07:00:00.000Z","2022-08-17T07:00:00.000Z",
    "ICIBAN","NFO","futures","2022-08-25T07:00:00.000Z","others","0"
);        
```

<a href="#index">Back to Index</a>

<h4 id="historical_data_v22">Get Historical Data (version 2) for Equity</h4>


```java
breezeConnect.getHistoricalDatav2(
    "1minute","2022-08-15T07:00:00.000Z","2022-08-17T07:00:00.000Z",
    "ITC","NSE","cash"
);
```

<a href="#index">Back to Index</a>
<h4 id="historical_data_v23">Get Historical Data (version 2) for Options</h4>


```java

breezeConnect.getHistoricalDatav2(
    "1minute","2022-08-15T07:00:00.000Z","2022-08-17T07:00:00.000Z",
    "CNXBAN","NFO","options","2022-09-29T07:00:00.000Z","call","38000"
);
```


<p> 
Note : 

1) Get Historical Data (version 2) for specific stock-code by mentioning interval either as "1second","1minute", "5minute", "30minute" or as "1day". 

2) Maximum candle intervals in one single request is 1000

</p>
<br>
<a href="#index">Back to Index</a>
<hr>


<h4 id="add_margin">Add Margin to your account.</h4>


```java
breezeConnect.addMargin(
    "margin", "ICIBAN", "BSE", "2021220", "100", "3817.10", "10", "0", "", "", 
    "", "", "", ""
);
```


<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="get_margin">Get Margin of your account.</h4>


```java
breezeConnect.getMargin("NSE");

```

<p> Note: Please change exchange_code=“NFO” to get F&O margin details </p>
<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="place_order">Placing a Futures Order from your account.</h4>


```java
breezeConnect.placeOrder(
    "ICIBAN","NFO","futures","buy","limit","0","3200","200","day",
    "2022-08-22T06:00:00.000Z","0","2022-08-25T06:00:00.000Z","others",
    "0","Test"
);
```                    


<h4 id="place_order2">Placing an Option Order from your account.</h4>


```java
breezeConnect.placeOrder(
    "NIFTY","NFO","options","buy","market","","50","","day","2022-08-30T06:00:00.000Z",
    "0","2022-09-29T06:00:00.000Z","call","16600"
);
```


<br>
<a href="#index">Back to Index</a>

<h4 id="place_order3">Place a cash order from your account.</h4>


```java
breezeConnect.placeOrder(
    "ITC","NSE","cash","buy","limit","","1","305","day"
);
```                

<br>
<a href="#index">Back to Index</a>

<h4 id="place_order4">Place an optionplus order</h4>

```java

breezeConnect.placeOrder(
    "NIFTY","NFO","optionplus","buy","limit","15","50","11.25","day",
    "2022-12-02T06:00:00.000Z","0","2022-12-08T06:00:00.000Z","call",
    "19000","Limit","20","Test"
);
```                
<br>
<a href="#index">Back to Index</a>

<h4 id="place_order5">Place an future plus order</h4>

```java

breezeConnect.placeOrder( 
    "NIFTY","NFO","futureplus","Buy","limit","18720",
    "50","18725","Day","0","29-DEC-2022"
);
```                
<br>
<p>Future plus - "Stop loss trigger price cannot be less than last traded price for Buy order" </p>
<a href="#index">Back to Index</a>

<hr>

<h4 id="order_detail">Get an order details by exchange-code and order-id from your account.</h4>

```java
breezeConnect.getOrderDetail("NSE","20220819N100000001");
```                        

<p> Note: Please change exchange_code=“NFO” to get details about F&O</p>
<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="order_list">Get order list of your account.</h4>


```java
breezeConnect.getOrderList("NSE","2022-08-01T10:00:00.000Z","2022-08-19T10:00:00.000Z");
```

<p> Note: Please change exchange_code=“NFO” to get details about F&O</p>
<br>
<a href="#index">Back to Index</a>
<hr>


<h4 id="cancel_order">Cancel an order from your account whose status are not Executed.</h4> 


```java
breezeConnect.cancelOrder("NSE","20220819N100000001");
```                    

<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="modify_order">Modify an order from your account whose status are not Executed.</h4> 


```java
breezeConnect.modifyOrder(
    "202208191100000001","NFO","limit","0","250","290100","day",
    "0","2022-08-22T06:00:00.000Z"
);
```

<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="portfolio_holding">Get Portfolio Holdings of your account.</h4>


```java
breezeConnect.getPortfolioHoldings(
    "NFO","2022-08-01T06:00:00.000Z","2022-08-19T06:00:00.000Z","",""
);
```

<p> Note: Please change exchange_code=“NSE” to get Equity Portfolio Holdings</p>
<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="portfolio_position">Get Portfolio Positions from your account.</h4>


```java
breezeConnect.getPortfolioPositions()

```

<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="get_quotes">Get quotes of mentioned stock-code </h4>


```java
breezeConnect.getQuotes(
    "ICIBAN","NFO","2022-08-25T06:00:00.000Z","futures",
    "others","0"
);
```

<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="get_option_chain">Get option-chain of mentioned stock-code for product-type Futures where input of expiry-date is not compulsory</h4>


```java
breezeConnect.getOptionChainQuotes(
    "ICIBAN","NFO","futures","2022-08-25T06:00:00.000Z"
);
```                    

<br>
<a href="#index">Back to Index</a>

<h4 id="get_option_chain2">Get option-chain of mentioned stock-code for product-type Options where atleast 2 input is required out of expiry-date, right and strike-price</h4>


```java
breezeConnect.getOptionChainQuotes(
    "ICIBAN","NFO","options","2022-08-25T06:00:00.000Z","call","16850"
);
```

<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="square_off1">Square off an Equity Margin Order</h4>


```java
breezeConnect.squareOff(
    "NSE","margin","NIFTY","10","0","sell","market","day","0","0",
    "","","","",""
);
```

<p> Note: Please refer get_portfolio_positions() for settlement id and margin_amount</p>
<br>
<a href="#index">Back to Index</a>

<h4 id="square_off2">Square off an FNO Futures Order</h4>


```java
breezeConnect.squareOff(
    "NFO","futures","ICIBAN","2022-08-25T06:00:00.000Z","sell","market",
    "day","0","50","0","2022-08-12T06:00:00.000Z","","0"
);
```

<br>
<a href="#index">Back to Index</a>

<h4 id="square_off3">Square off an FNO Options Order</h4>


```java
breezeConnect.squareOff(
    "NFO","options","ICIBAN","2022-08-25T06:00:00.000Z","Call","16850",
    "sell","market","day","0","50","0","2022-08-12T06:00:00.000Z","","0"
);
```                    

<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="trade_list">Get trade list of your account.</h4>


```java
breezeConnect.getTradeList(
    "2022-08-01T06:00:00.000Z","2022-08-19T06:00:00.000Z","NSE",
    "","","");
```                        

<p> Note: Please change exchange_code=“NFO” to get details about F&O</p>
<br>
<a href="#index">Back to Index</a>
<hr>

<h4 id="trade_detail">Get trade detail of your account.</h4>


```java
breezeConnect.getTradeDetail("NSE","20220819N100000005");
```

<p> Note: Please change exchange_code=“NFO” to get details about F&O</p>
<br>
<a href="#index">Back to Index</a>
<hr>


<h4 id = "get_names">Get Names </h4>


```java
breezeConnect.getNames('NSE','TATASTEEL');
breezeConnect.getNames('NSE','RELIANCE');
```
<p>Note: Use this method to find ICICI specific stock codes / token </p>

<a href="#index">Back to Index</a>

<hr>

<h4 id = "preview_order">Preview Order </h4>

```java

breezeConnect.previewOrder("ICIBAN","NSE", "margin","limit","907.05","buy","1","N");

```
<a href="#index">Back to Index</a>