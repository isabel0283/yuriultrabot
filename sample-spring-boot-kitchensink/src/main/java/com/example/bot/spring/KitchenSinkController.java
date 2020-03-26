/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.example.bot.spring;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.google.common.io.ByteStreams;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.BeaconEvent;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.JoinEvent;
import com.linecorp.bot.model.event.MemberJoinedEvent;
import com.linecorp.bot.model.event.MemberLeftEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.UnfollowEvent;
import com.linecorp.bot.model.event.message.FileMessageContent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.LocationMessageContent;
import com.linecorp.bot.model.event.message.StickerMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.message.VideoMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.LocationMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LineMessageHandler
public class KitchenSinkController {
    @Autowired
    private LineMessagingClient lineMessagingClient;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        TextMessageContent message = event.getMessage();
        handleTextContent(event.getReplyToken(), event, message);
    }

    @EventMapping
    public void handleLocationMessageEvent(MessageEvent<LocationMessageContent> event) {
        LocationMessageContent locationMessage = event.getMessage();
        reply(event.getReplyToken(), new LocationMessage(
                locationMessage.getTitle(),
                locationMessage.getAddress(),
                locationMessage.getLatitude(),
                locationMessage.getLongitude()
        ));
    }

    @EventMapping
    public void handleFileMessageEvent(MessageEvent<FileMessageContent> event) {
        this.reply(event.getReplyToken(),
                   new TextMessage(String.format("Received '%s'(%d bytes)",
                                                 event.getMessage().getFileName(),
                                                 event.getMessage().getFileSize())));
    }

    @EventMapping
    public void handleImageEvent(MessageEvent<ImageMessageContent> event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "I saw that before...");
    }

    @EventMapping
    public void handleVideoEvent(MessageEvent<VideoMessageContent> event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "That took a life to upload...");
    }

    @EventMapping
    public void handleUnfollowEvent(UnfollowEvent event) {
        log.info("unfollowed this bot: {}", event);
    }

    @EventMapping
    public void handleFollowEvent(FollowEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Got followed event");
    }

    @EventMapping
    public void handleJoinEvent(JoinEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Thanks for adding me to your group!");
    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken,
                       "Got postback data " + event.getPostbackContent().getData() + ", param " + event
                               .getPostbackContent().getParams().toString());
    }

    @EventMapping
    public void handleBeaconEvent(BeaconEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Got beacon message " + event.getBeacon().getHwid());
    }

    @EventMapping
    public void handleMemberJoined(MemberJoinedEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Welcome to this group! I'll try to not bothering you so much...");
    }

    @EventMapping
    public void handleMemberLeft(MemberLeftEvent event) {
        log.info("Got memberLeft message: {}", event.getLeft().getMembers()
                .stream().map(Source::getUserId)
                .collect(Collectors.joining(",")));
    }

    @EventMapping
    public void handleOtherEvent(Event event) {
        log.info("Received message(Ignored): {}", event);
    }

    private void reply(@NonNull String replyToken, @NonNull Message message) {
        reply(replyToken, Collections.singletonList(message));
    }

    private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
        try {
            BotApiResponse apiResponse = lineMessagingClient
                    .replyMessage(new ReplyMessage(replyToken, messages))
                    .get();
            log.info("Sent messages: {}", apiResponse);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void replyText(@NonNull String replyToken, @NonNull String message) {
        if (replyToken.isEmpty()) {
            throw new IllegalArgumentException("replyToken must not be empty");
        }
        if (message.length() > 1000) {
            message = message.substring(0, 1000 - 2) + "……";
        }
        this.reply(replyToken, new TextMessage(message));
    }

    private void handleHeavyContent(String replyToken, String messageId,
                                    Consumer<MessageContentResponse> messageConsumer) {
        final MessageContentResponse response;
        try {
            response = lineMessagingClient.getMessageContent(messageId)
                                          .get();
        } catch (InterruptedException | ExecutionException e) {
            reply(replyToken, new TextMessage("Cannot get image: " + e.getMessage()));
            throw new RuntimeException(e);
        }
        messageConsumer.accept(response);
    }

    private void handleSticker(String replyToken, StickerMessageContent content) {
        reply(replyToken, new StickerMessage(
                content.getPackageId(), content.getStickerId())
        );
    }

    private void handleTextContent(String replyToken, Event event, TextMessageContent content)
            throws Exception {
        String textOriginal = content.getText();
        String textClear = textOriginal.trim();
        String text = textClear.toLowerCase();
        String strOrig = text;
        int intIndex = strOrig.indexOf("test");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "test";
        }
        intIndex = strOrig.indexOf("food");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "food";
         }
        intIndex = strOrig.indexOf("sex");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "sex";
        }
        intIndex = strOrig.indexOf("lurk");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "lurking";
         }
        intIndex = strOrig.indexOf("love");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "love";
         }
        intIndex = strOrig.indexOf("don't care");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "dontcare";
         }
        intIndex = strOrig.indexOf("thanks yuri");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "thanksYuri";
         }
         intIndex = strOrig.indexOf("lol");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "hahaha";
         }
         intIndex = strOrig.indexOf("yuri weather");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "weatherYuri";
         }
         intIndex = strOrig.indexOf("work");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "work";
         }
         intIndex = strOrig.indexOf("yuri i love you");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "yuriiloveyou";
         }
         intIndex = strOrig.indexOf("haha");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "hahaha";
         }
         intIndex = strOrig.indexOf("bye yuri");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "bye yuri";
         }
         intIndex = strOrig.indexOf("yuri youtube");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "youtubeYuri";
         }
        log.info("Got text message from replyToken:{}: text:{}", replyToken, text);
        Random randWait = new Random();
        int myrandWait = 0;
        myrandWait = randWait.nextInt(5) + 1;
        TimeUnit.SECONDS.sleep(myrandWait);
        Random rand = new Random();
        String strRandom = "";
        int myrandInt = 0;
        String message = "...";
        switch (text) {
            case "youtube":
                log.info("Returns echo message {}: {}", replyToken, text);
                message = "You could ask me to search for a video, like this: Yuri Youtube kurt cobain";
                this.replyText(replyToken, message);
                break;
            case "weather":
                log.info("Returns echo message {}: {}", replyToken, text);
                message = "Ask me the current weather in your city, like this: Yuri weather kurashiki";
                this.replyText(replyToken, message);
                break;
            case "youtubeYuri":
                if ("youtube".equals(strOrig)) {
                    break;
                }
                String emptyString = " ";
                String keyword = strOrig.replace("youtube", "");
                keyword = keyword.replace("yuri", "");
                if (emptyString.equals(keyword)) {
                    this.replyText(replyToken, "Gomen ne! I need more information...");
                    break;
                }
                keyword = keyword.replace(" ", "+");
                String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=1&order=relevance&q=" + keyword + "&key=AIzaSyAkW1C_AMaU60nOpJunpMAu9RLq09l84Ms";
                Document result = Jsoup.connect(url)
                    .userAgent("Mozilla")
                    .timeout(3000)
                    .ignoreContentType(true)
                    .get();
                String getJson = result.text();
                JSONObject jsonObject = (JSONObject) new JSONTokener(getJson).nextValue();
                JSONArray mainArray = jsonObject.getJSONArray("items");
                JSONObject subjsonobj = mainArray.getJSONObject(0);
                String video = subjsonobj.getJSONObject("id").getString("videoId");
                this.replyText(replyToken, "https://youtu.be/" + video);
                break;
             case "weatherYuri":
                if ("weather".equals(strOrig)) {
                    break;
                }
                String wemptyString = " ";
                String keywordCity = strOrig.replace("weather", "");
                keywordCity = keywordCity.replace("yuri", "");
                if (wemptyString.equals(keywordCity)) {
                    this.replyText(replyToken, "I'm sorry! Please specify a city!");
                    break;
                }
                keywordCity = keywordCity.replace(" ", "");
                String wurl = "http://api.openweathermap.org/data/2.5/weather?q=" + keywordCity + "&units=metric&appid=42df99363e6213b72d9bec95685299a2";
                Document wresult = Jsoup.connect(wurl)
                    .userAgent("Mozilla")
                    .timeout(3000)
                    .ignoreContentType(true)
                    .get();
                String wgetJson = wresult.text();
                //next line for control delete when done;
                //this.replyText(replyToken, wgetJson);
                JSONObject wjsonObject = (JSONObject) new JSONTokener(wgetJson).nextValue();
                //JSONArray wmainArray = wjsonObject.getJSONArray("weather");
                //JSONObject wsubjsonobj = wmainArray.getJSONObject(0);
                String yourTemp = wjsonObject.getJSONObject("main").getString("humidity");
                this.replyText(replyToken, "this is" + yourTemp);
                String yourCountry = wjsonObject.getJSONObject("sys").getString("country");
                String yourCity = wjsonObject.getString("name");
                //this.replyText(replyToken, yourTemp);
                //String yourTemp = wjsonObject.getString("temp");
                //String yourTemp = "37";
                String yourCondition = "Clouds";
                String yourConditionDesc = "Clouds all over the world";
                String wreport = "The current weather conditions in ";
                this.replyText(replyToken, "Report" + yourCity + ", " + yourCountry + " is " + yourTemp);
                break;
            case "yuri show me something pretty": {
                log.info("Invoking 'profile' command: source:{}",
                         event.getSource());
                String userId = event.getSource().getUserId();
                if (userId != null) {
                    if (event.getSource() instanceof GroupSource) {
                        lineMessagingClient
                                .getGroupMemberProfile(((GroupSource) event.getSource()).getGroupId(), userId)
                                .whenComplete((profile, throwable) -> {
                                    if (throwable != null) {
                                        this.replyText(replyToken, throwable.getMessage());
                                        return;
                                    }

                                    this.reply(
                                            replyToken,
                                            Arrays.asList(new TextMessage("Like this? :)"),
                                                          new ImageMessage(profile.getPictureUrl(),
                                                                           profile.getPictureUrl()))
                                    );
                                });
                    } else {
                        lineMessagingClient
                                .getProfile(userId)
                                .whenComplete((profile, throwable) -> {
                                    if (throwable != null) {
                                        this.replyText(replyToken, throwable.getMessage());
                                        return;
                                    }

                                    this.reply(
                                            replyToken,
                                            Arrays.asList(new TextMessage(
                                                                  "Display name: " + profile.getDisplayName()),
                                                          new TextMessage("Status message: "
                                                                          + profile.getStatusMessage()))
                                    );

                                });
                    }
                } else {
                    this.replyText(replyToken, "I don't think so...");
                }
                break;
            }
            case "bye yuri": {
                Source source = event.getSource();
                if (source instanceof GroupSource) {
                    this.replyText(replyToken, "I don't think so...");
                    //lineMessagingClient.leaveGroup(((GroupSource) source).getGroupId()).get();
                } else if (source instanceof RoomSource) {
                    this.replyText(replyToken, "Leaving room");
                    lineMessagingClient.leaveRoom(((RoomSource) source).getRoomId()).get();
                } else {
                    this.replyText(replyToken, "Bot can't leave from 1:1 chat");
                }
                break;
            }
            case "alter_flex":
                this.reply(replyToken, new ExampleFlexMessageSupplier().get());
                break;
            case "quickreply":
                this.reply(replyToken, new MessageWithQuickReplySupplier().get());
                break;
            case "yuri": {
                myrandInt = rand.nextInt(10);
                log.info("Returns echo message {}: {}", replyToken, text);
                strRandom = "" + myrandInt;
                switch (strRandom) {
                       case "0":
                           message = "Yes?";
                           break;
                       case "1":
                           message = "What?";
                           break;
                       case "2":
                           message = "Stop saying my name!";
                           break;
                       case "3":
                           message = "Don't say my name!";
                           break;
                       case "4":
                           message = "Why are you calling me?";
                           break;
                       case "5":
                           message = "Dont talk to me!";
                           break;
                       case "6":
                           message = "What do you want?";
                           break;
                       case "7":
                           message = "I don't have time for you!";
                           break;
                       case "8":
                           message = "Don't you have anything else to do?";
                           break;
                       case "9":
                           message = "Go get a life!";
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
             }
             case "work": {
                myrandInt = rand.nextInt(15);
                log.info("Returns echo message {}: {}", replyToken, text);
                strRandom = "" + myrandInt;
                switch (strRandom) {
                       case "0":
                           message = "At least you have work, no?";
                           break;
                       case "1":
                           message = "Don't complain...";
                           break;
                       case "2":
                           message = "You could be ding something better...";
                           break;
                       case "3":
                           message = "Realy?";
                           break;
                       case "4":
                           message = "I've heard that before...";
                           break;
                       case "5":
                           message = "Ahhh ok...";
                           break;
                       case "6":
                           message = "I'd preffer doing something else...";
                           break;
                       case "7":
                           message = "Why don't you try something different?";
                           break;
                       case "8":
                           message = "If you say so...";
                           break;
                       case "9":
                           message = "yeah... so do I...";
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
             }
            case "love": {
                myrandInt = rand.nextInt(15);
                log.info("Returns echo message {}: {}", replyToken, text);
                strRandom = "" + myrandInt;
                switch (strRandom) {
                       case "0":
                           message = "Do you believe in that?";
                           break;
                       case "1":
                           message = "You'll regret later... You'll see!";
                           break;
                       case "2":
                           message = "Love yourself!";
                           break;
                       case "3":
                           message = "I was wondering if that's really true...";
                           break;
                       case "4":
                           message = "Such thing doesn't exist...";
                           break;
                       case "5":
                           message = "...";
                           break;
                       case "6":
                           message = "Stay away, it's better...";
                           break;
                       case "7":
                           message = "You really like suffering? Isn't";
                           break;
                       case "8":
                           message = "*Sighs*";
                           break;
                       case "9":
                           message = "I'd rather be alone...";
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
             }
            case "test":
                log.info("Returns echo message {}: {}", replyToken, text);
                rand = new Random();
                myrandInt = rand.nextInt(5);
                this.replyText(replyToken, "The number is... " + myrandInt);
                break;
            case "alter_bot":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.replyText(replyToken, "I'm glad I'm not a human!");
                break;
            case "dontcare":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.replyText(replyToken, "Fine!");
                break;
            case "thanksYuri":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.replyText(replyToken, "You welcome!");
                break;
            case "food":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.replyText(replyToken, "Suddenly I feel hungry... :(");
                break;
            case "sex":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.replyText(replyToken, "Don't you have anything else to talk about?");
                break;
            case "yuriiloveyou":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.replyText(replyToken, "I don't care...");
                break;
            case "hahaha": {
                myrandInt = rand.nextInt(15);
                log.info("Returns echo message {}: {}", replyToken, text);
                strRandom = "" + myrandInt;
                switch (strRandom) {
                       case "0":
                           message = "That's not funny at all...";
                           break;
                       case "1":
                           message = "My wall is funnier...";
                           break;
                       case "2":
                           message = "You are making us feel uncomfortable...";
                           break;
                       case "3":
                           message = "Hahaha! that's so pathetic...";
                           break;
                       case "4":
                           message = "Why are you laughing?";
                           break;
                       case "5":
                           message = "Hey! that was close to be funny... Just close...";
                           break;
                       case "6":
                           message = "Duh...";
                           break;
                       case "7":
                           message = "Ehhh... What was that?";
                           break;
                       case "8":
                           message = "That's not even close to funny...";
                           break;
                       case "9":
                           message = "Was that really a joke?";
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
            }
            case "no": {
                myrandInt = rand.nextInt(10);
                log.info("Returns echo message {}: {}", replyToken, text);
                strRandom = "" + myrandInt;
                switch (strRandom) {
                       case "0":
                           message = "Why not?";
                           break;
                       case "1":
                           message = "Are you sure?";
                           break;
                       case "2":
                           message = "Ok...";
                           break;
                       case "3":
                           message = "If you say so...";
                           break;
                       case "4":
                           message = "Agree!";
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
            }
            case "questionYuri":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.reply(replyToken, new StickerMessage("11539", "52114129"));
                break;
            case "lurking":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.replyText(replyToken, "Thats me... Any problem with that?");
                break;
            default:
                break;
        }
    }

    private static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                                          .path(path).build()
                                          .toUriString();
    }

    private void system(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        try {
            Process start = processBuilder.start();
            int i = start.waitFor();
            log.info("result: {} =>  {}", Arrays.toString(args), i);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
        log.info("Got content-type: {}", responseBody);
        DownloadedContent tempFile = createTempFile(ext);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            ByteStreams.copy(responseBody.getStream(), outputStream);
            log.info("Saved {}: {}", ext, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DownloadedContent createTempFile(String ext) {
        String fileName = LocalDateTime.now().toString() + '-' + UUID.randomUUID().toString() + '.' + ext;
        Path tempFile = KitchenSinkApplication.downloadedContentDir.resolve(fileName);
        tempFile.toFile().deleteOnExit();
        return new DownloadedContent(
                tempFile,
                createUri("/downloaded/" + tempFile.getFileName()));
    }

    @Value
    public static class DownloadedContent {
        Path path;
        String uri;
    }
}
