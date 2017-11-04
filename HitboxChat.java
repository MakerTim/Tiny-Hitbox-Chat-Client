/*
Depends on:
io.socket: 					socket.io-client 	version: '1.0.0'
org.apache.httpcomponents:	httpclient			version: '4.5.3'
com.google.code.gson:		gson				version: '2.8.0'
(org.projectlombok: 		lombok: 			version: '1.16.18')
*/

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.socket.client.IO;
import io.socket.client.Socket;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Tim Biesenbeek
 */
public class HitboxChat {

	public static void main(String[] args) throws Exception {
		CloseableHttpClient client = HttpClientBuilder.create().build();
		CloseableHttpResponse response = client.execute(new HttpGet("https://api.smashcast.tv/chat/servers"));
		HttpEntity entity = response.getEntity();
		JsonArray sites = new JsonParser().parse(EntityUtils.toString(entity)).getAsJsonArray();

		String siteString = "";

		for (int i = 0; i < sites.size(); i++) {
			JsonObject site = sites.get(i).getAsJsonObject();
			String chatURL = site.get("server_ip").getAsString();
			try {
				InetAddress.getByName(chatURL);
				siteString = chatURL;
				break;
			} catch (Exception ex) {
				System.err.println(ex.toString());
			}
		}
		System.out.println(siteString);
		next(sites, -1);
	}

	public static void next(JsonArray sites, int i) {
		i++;
		i %= sites.size();
		final int j = i;
		System.err.println(j);

		IO.Options opts = new IO.Options();
		opts.timeout = 15000;
		opts.transports = new String[]{"websocket"};

		Socket sock;
		try {
			String site = "https://" + sites.get(i).getAsJsonObject().get("server_ip").getAsString();
			System.out.println(site);
			sock = IO.socket(site, opts);
		} catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}

		sock.on(Socket.EVENT_CONNECT, args -> {
			try {
				JSONObject request = new JSONObject();
				request.put("method", "joinChannel");
				JSONObject params = new JSONObject();
				params.put("channel", "MakerTim");
				params.put("name", "UnknownSoldier");
				params.put("token", JSONObject.NULL);
				params.put("hideBuffered", false);
				request.put("params", params);
				sock.send(request);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			System.out.println("Hey Listen");
		}).on(Socket.EVENT_MESSAGE, args -> {
			if (args.length == 0) {
				return;
			}
			JsonObject msgObj = new JsonParser().parse(args[0].toString()).getAsJsonObject();
			JsonObject params = msgObj.get("params").getAsJsonObject();

			// VARIABLES
			if (params.has("text") && params.has("id")) {
				String channel = params.get("channel").getAsString();
				String user = params.get("name").getAsString();
				String color = params.get("nameColor").getAsString();
				String message = params.get("text").getAsString();
				Date when = new Date(params.get("time").getAsLong());
				String id = params.get("id").getAsString();
				String role = params.get("role").getAsString(); // admin,user
				boolean isFollower = params.get("isFollower").getAsBoolean();
				boolean isSubscriber = params.get("isSubscriber").getAsBoolean();
				boolean isOwner = params.get("isOwner").getAsBoolean();
				boolean isStaff = params.get("isStaff").getAsBoolean();
				boolean isCommunity = params.get("isCommunity").getAsBoolean();
				boolean media = params.get("media").getAsBoolean();

				ChatMessage chatMessage = new ChatMessage(channel, user, color, message, when, id, role, isFollower,
						isSubscriber, isOwner, isStaff, isCommunity, media);

				System.out.printf("%s: %s\n", chatMessage.getUser(), chatMessage.getMessage());
			}
		});
		sock.connect();
	}

	@Data
	@AllArgsConstructor
	public static class ChatMessage {
		private final String channel;
		private final String user;
		private final String color;
		private final String message;
		private final Date when;
		private final String id;
		private final String role;
		private final boolean isFollower;
		private final boolean isSubscriber;
		private final boolean isOwner;
		private final boolean isStaff;
		private final boolean isCommunity;
		private final boolean media;
	}
}