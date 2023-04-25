package com.breeze.breezeconnect;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

public class Test {
	public static void main(String[] args) throws IOException {
		try {

			////////////////////////Initiate////////////////////////
			JSONObject creds = new JSONObject("{\"vk\":{\"password\":\"test1234\",\"dOB\":\"25051982\",\"appKey\":\"#aL9488x480^5E0744ws96969xZ@4GB2\",\"secretKey\":\"98a#305@7M1442yk6c9105CwY0956157\",\"idirect_Userid\":\"9833288070\"}}");
			BreezeConnect breezeConnect = new BreezeConnect(creds.getJSONObject("vk").getString("appKey"));
			String apiSession = getApiSession(creds,"vk");
			breezeConnect.generateSession(creds.getJSONObject("vk").getString("secretKey"),apiSession);
			//////////////////////Initiate End//////////////////////

			////////////////////////WebSocket////////////////////////
		//	breezeConnect.connectTicker();
		//	breezeConnect.subscribeFeeds("13.1!9739");
//			breezeConnect.subscribeFeeds("13.1!9739","1second");
//			breezeConnect.subscribeFeeds("NSE","INFTEC","","","","",true,false);
//			breezeConnect.watch(new String[] {"4.1!15083","4.2!11483","4.1!TCSEQ", "4.1!49937","4.1!11287"});
//					breezeConnect.unWatch(new String[]{"4.2!11483","4.1!TCSEQ"});
			/*breezeConnect.registerOnTickEventListener(new OnTickEventListener() {
				@Override
				public void onTickEvent(Object tick) {
					System.out.println("Ticker data:");
					System.out.println(tick.toString());
				}
			});*/
			//////////////////////WebSocket End//////////////////////

			///////////////////////////API///////////////////////////
//			System.out.println(breezeConnect.getCustomerDetails(apiSession).toString());
//			System.out.println(breezeConnect.getDematHoldings());  // RNA Error
			System.out.println(breezeConnect.getFunds());
//			System.out.println(breezeConnect.getPortfolioPositions());
			System.out.println(breezeConnect.previewOrder("ICIBAN","NSE","margin","limit","907.05","buy","1","","","","N","",""));
//			System.out.println(breezeConnect.setFunds("debit","200","Equity"));
//			System.out.println(breezeConnect.getHistoricalDatav2("1minute","2022-04-01T00:00:00.000Z","2022-05-31T00:00:00.000Z","INFTEC","NSE","","","",""));
//			System.out.println(breezeConnect.getHistoricalData("1minute","2021-11-15T07:00:00.000Z","2021-11-17T07:00:00.000Z",
//						"AXIBAN","NFO","futures","2021-11-25T07:00:00.000Z","others","0"));
//			System.out.println(breezeConnect.getMargin("NSE"));
//			System.out.println(breezeConnect.addMargin("cash","ITC","NSE","2022106","100",
//					"265","1","0","","","","","","N")); //RNA
//			System.out.println(breezeConnect.getOrderList("NFO","2022-06-01T00:00:00.000Z","2022-06-10T00:00:00.000Z")); // 500 Error
//			System.out.println(breezeConnect.getPortfolioHoldings("NSE","2022-05-01T06:00:00.000Z","2022-05-30T06:00:00.000Z",
//					"","")); // Error code 500
//			System.out.println(breezeConnect.getQuotes("NIFTY","NFO","2022-06-10T00:00:00.000Z","Options",
//					"Others","0")); // Application error
//			System.out.println(breezeConnect.getTradeList("2021-09-28T06:00:00.000Z","2021-11-15T06:00:00.000Z","NSE","Cash",
//					"buy","INFTEC"));

//			System.out.println(breezeConnect.placeOrder("NIFTY","NFO","Options","Buy","Market","0","50",
//					"","Day","2022-06-12T00:00:00.000Z","0","2022-06-12T00:00:00.000Z","Call","17800",
//					"test"));
//			System.out.println(breezeConnect.getTradeList("2022-06-06T06:00:00.000Z","2022-06-09T06:00:00.000Z","NSE","","",""));
			/////////////////////////API End/////////////////////////
//			System.out.println(breezeConnect.modifyOrder("20220607N100000015","NSE","Market","0","5",
//					"","ioc","0",""));
		}catch (JSONException e){
			e.printStackTrace();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String getApiSession(JSONObject creds, String userId){
		try{
			JSONObject requestBody = new JSONObject(creds.getJSONObject(userId).toString());
			CloseableHttpClient client = HttpClients.createDefault();
			HttpGetWithEntity httpGet = new HttpGetWithEntity();
			StringEntity requestEntity = new StringEntity(requestBody.toString());
			httpGet.setURI(URI.create("https://api.icicidirect.com/breezeapi/api/v1/customerlogin"));
			httpGet.setEntity(requestEntity);
			httpGet.setHeader("Accept", "application/json");
			httpGet.setHeader("Content-type", "application/json");
			CloseableHttpResponse response = client.execute(httpGet);
			client.close();
			HttpEntity responseEntity = response.getEntity();
			String responseString = EntityUtils.toString(responseEntity, "UTF-8");
			JSONObject responseJson = new JSONObject(responseString);
			return responseJson.getInt("Status")== 500 ? "" :responseJson.getJSONObject("Success").getString("API_Session");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}