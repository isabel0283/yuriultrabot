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
                   new TextMessage(String.format("Thanks for sharing!",
                                                 event.getMessage().getFileName(),
                                                 event.getMessage().getFileSize())));
    }

    @EventMapping
    public void handleImageEvent(MessageEvent<ImageMessageContent> event) {
        String replyToken = event.getReplyToken();
        String messageimg = "Thanks for sharing!";
        this.replyText(replyToken, messageimg);
    }

    @EventMapping
    public void handleVideoEvent(MessageEvent<VideoMessageContent> event) {
        String replyToken = event.getReplyToken();
        String messagevid = "Thanks for sharing!";
        this.replyText(replyToken, messagevid);
    }

    @EventMapping
    public void handleUnfollowEvent(UnfollowEvent event) {
        log.info("unfollowed this bot: {}", event);
    }

    @EventMapping
    public void handleFollowEvent(FollowEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Now what?");
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
        this.replyText(replyToken, "Welcome to this group! Rules are basic: respect each other and have fun!");
    }

    @EventMapping
    public void handleMemberLeft(MemberLeftEvent event) {
        log.info("Got memberLeft message: {}", event.getLeft().getMembers()
                                                    .stream().map(Source::getUserId)
                                                    .collect(Collectors.joining(",")));
        //String replyToken = event.getReplyToken();
        //this.replyText(replyToken, "Good luck! Bye!");
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
        Integer intIndex = strOrig.indexOf("food");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "food";
         }
        intIndex = strOrig.indexOf("why?");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "questionwhy";
         }
        intIndex = strOrig.indexOf("cook");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "food";
         }
        intIndex = strOrig.indexOf("bot");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "yukobot";
         }
        intIndex = strOrig.indexOf("bye");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "bye";
         }
        intIndex = strOrig.indexOf("why not");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "whynot";
         }
        intIndex = strOrig.indexOf("sex");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "sex";
        }
        intIndex = strOrig.indexOf("video");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "video";
        }
        intIndex = strOrig.indexOf("me?");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "questionme";
        }
        intIndex = strOrig.indexOf("you?");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "questionyou";
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
        intIndex = strOrig.indexOf("thanks yuko");
        if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "thanksYuko";
         }
         intIndex = strOrig.indexOf("lol");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "hahaha";
         }
         intIndex = strOrig.indexOf("yuko weather");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "weatherYuko";
         }
         intIndex = strOrig.indexOf("work");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "work";
         }
         intIndex = strOrig.indexOf("yuko i love you");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "yukoiloveyou";
         }
         intIndex = strOrig.indexOf("haha");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "hahaha";
         }
         intIndex = strOrig.indexOf("kkk");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "hahaha";
         }
         intIndex = strOrig.indexOf("bye yuko");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "bye yuko";
         }
         intIndex = strOrig.indexOf("yuko youtube");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "youtubeYuko";
         }
         intIndex = strOrig.indexOf("yuko advice");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "adviceYuko";
         }
         intIndex = strOrig.indexOf("yuko flirt with");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "yukoflirt";
         }
        intIndex = strOrig.indexOf("yuko please stop");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "stopYuko";
         }
         intIndex = strOrig.indexOf("yuko stop");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "stopYuko";
         }
         intIndex = strOrig.indexOf("stop yuko");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "stopYuko";
         }
         intIndex = strOrig.indexOf("liar");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "liar";
         }
         intIndex = strOrig.indexOf("hello");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "hello";
         }
         intIndex = strOrig.indexOf("i know");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "iknow";
         }
         intIndex = strOrig.indexOf("how are you");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "howareyou";
         }
         intIndex = strOrig.indexOf("what are you");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "whatareyou";
         }
         intIndex = strOrig.indexOf("bob quiz");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "bob";
         }
         intIndex = strOrig.indexOf("robby");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "bob";
         }
         intIndex = strOrig.indexOf("again");
         if (intIndex == -1) {
                intIndex = 0;
         } else {
                text = "again";
         }
        log.info("Got text message from replyToken:{}: text:{}", replyToken, text);
        Random randWait = new Random();
        int myrandWait = 0;
        myrandWait = randWait.nextInt(2) + 1;
        TimeUnit.SECONDS.sleep(myrandWait);
        Random rand = new Random();
        String strRandom = "";
        int myrandInt = 0;
        String message = "";
        switch (text) {
            case "youtube":
                log.info("Returns echo message {}: {}", replyToken, text);
                message = "You could ask me to search for a video, like this: Yuko Youtube kurt cobain";
                this.replyText(replyToken, message);
                break;
            case "weather":
                log.info("Returns echo message {}: {}", replyToken, text);
                message = "Ask me the current weather in your city, like this: Yuko weather Kurashiki";
                this.replyText(replyToken, message);
                break;
            case "youtubeYuko":
                if ("youtube".equals(strOrig)) {
                    break;
                }
                String emptyString = " ";
                String keyword = strOrig.replace("youtube", "");
                keyword = keyword.replace("yuko", "");
                if (emptyString.equals(keyword)) {
                    this.replyText(replyToken, "Gomen ne! I need more information...");
                    break;
                }
                keyword = keyword.replace(" ", "+");
                String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=1&order=relevance&q=" + keyword + "&key=AIzaSyC06VSmiAJkezeJqjQeqn20FJyxYvXacPc";
                Document result = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1) Chrome/41.0.2228.0 Safari/537.36")
                    .timeout(3000)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .get();
                String getJson = result.text();
                JSONObject jsonObject = (JSONObject) new JSONTokener(getJson).nextValue();
                JSONArray mainArray = jsonObject.getJSONArray("items");
                JSONObject subjsonobj = mainArray.getJSONObject(0);
                String video = subjsonobj.getJSONObject("id").getString("videoId");
                this.replyText(replyToken, "https://youtu.be/" + video);
                break;
            case "adviceYuko":
                String adviceurl = "https://api.adviceslip.com/advice";
                Document adviceresult = Jsoup.connect(adviceurl)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1) Chrome/41.0.2228.0 Safari/537.36")
                    .timeout(3000)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .get();
                String agetJson = adviceresult.text();
                JSONObject ajsonObject = new JSONObject(agetJson);
                String advice = ajsonObject.getJSONObject("slip").getString("advice");
                this.replyText(replyToken, advice);
                break;
             case "weatherYuko":
                if ("weather".equals(strOrig)) {
                    break;
                }
                String wemptyString = " ";
                String keywordCity = strOrig.replace("weather", "");
                keywordCity = keywordCity.replace("yuko", "");
                if (wemptyString.equals(keywordCity)) {
                    this.replyText(replyToken, "Please specify a city!");
                    break;
                }
                keywordCity = keywordCity.replace(" ", "+");
                String wurl = "http://api.openweathermap.org/data/2.5/weather?q=" + keywordCity + "&units=metric&appid=42df99363e6213b72d9bec95685299a2";
                Document wresult = Jsoup.connect(wurl)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1) Chrome/41.0.2228.0 Safari/537.36")
                    .timeout(3000)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .get();
                String wgetJson = wresult.text();
                int wintIndex = wgetJson.indexOf("city not found");
                if (wintIndex != -1) {
                    this.replyText(replyToken, "Couldn't find that or you requested a wrong city name.");
                    break;
                }
                JSONObject wjsonObject = new JSONObject(wgetJson);
                String yourCity = wjsonObject.getString("name");
                JSONObject sysDetails = wjsonObject.getJSONObject("sys");
                String yourCountry = sysDetails.getString("country");
                JSONArray weatherArray = wjsonObject.getJSONArray("weather");
                JSONObject weatherDetails = weatherArray.getJSONObject(0);
                String yourConditionDesc = weatherDetails.getString("description");
                JSONObject mainDetails = wjsonObject.getJSONObject("main");
                Double yourTemp = mainDetails.getDouble("temp");
                Double yourTempmin = mainDetails.getDouble("temp_min");
                Double yourTempmax = mainDetails.getDouble("temp_max");
                Double yourHumidity = mainDetails.getDouble("humidity");
                String yourCloudicon = weatherDetails.getString("icon");
                //Urls of icons
                String cloudIconurl = "https://openweathermap.org/img/wn/" + yourCloudicon + "@2x.png";
                final DownloadedContent jpg;
                final DownloadedContent previewImg;
                jpg = new DownloadedContent(null, cloudIconurl);
                previewImg = new DownloadedContent(null, cloudIconurl);
                //Building the report
                String wreportLocation = yourCity + ", " + yourCountry;
                String wreport = "We have " + yourConditionDesc + " in " + wreportLocation;
                wreport = wreport + ". \nThe current temperature is " + yourTemp;
                wreport = wreport + "°C, the lowest for today is " + yourTempmin;
                wreport = wreport + "°C and the highest is " + yourTempmax + "°C. ";
                wreport = wreport + "\nHumidity is at " + yourHumidity + "%.";
                this.reply(replyToken,
                           Arrays.asList(
                               new ImageMessage(jpg.getUri(), previewImg.getUri()),
                               new TextMessage(wreport)));
                break;
            case "bye yuko": {
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
            case "yukoflirt": {
                log.info("Returns echo message {}: {}", replyToken, text);
                myrandInt = rand.nextInt(25);
                strRandom = "" + myrandInt;
                String flirtTarget = strOrig.replaceAll("yuko flirt with ","");
                flirtTarget = flirtTarget.toUpperCase();
                switch (strRandom) {
                       case "0":
                           message = flirtTarget + ", if nothing lasts forever, will you be my nothing?";
                           break;
                       case "1":
                           message = flirtTarget + ", aside from being sexy, what do you do for a living?";
                           break;
                       case "2":
                           message = flirtTarget
                               + ", if I could rearrange the alphabet, I’d put ‘U’ and ‘I’ together.";
                           break;
                       case "3":
                           message = "I wish I were cross-eyed so I can see " + flirtTarget + " twice.";
                           break;
                       case "4":
                           message = "I must be in a museum, because " + flirtTarget
                               + " truly is a work of art.";
                           break;
                       case "5":
                           message = flirtTarget
                               + ", do you believe in love at first sight or should I walk by again?";
                           break;
                       case "6":
                           message = flirtTarget
                               + ", are you a time traveler? Cause I see you in my future!";
                           break;
                       case "7":
                           message = "Life without " + flirtTarget
                               + " is like a broken pencil... pointless.";
                           break;
                       case "8":
                           message = "Does someone here know CPR? Because " + flirtTarget
                               + " is taking my breath away!";
                           break;
                       case "9":
                           message = flirtTarget
                               + " must be tired because has been running through my mind all night.";
                           break;
                       case "10":
                           message = "Does someone here have a map? I keep getting lost in "
                               + flirtTarget + " eyes.";
                           break;
                       case "11":
                           message = "Did the sun come out or did "
                               + flirtTarget + " just smile at me?";
                           break;
                       case "12":
                           message = "My love for "
                               + flirtTarget + " is like diarrhea, I just can't hold it in.";
                           break;
                       case "13":
                           message = "Is this the Hogwarts Express? Because it feels like "
                               + flirtTarget + " and I are headed somewhere magical.";
                           break;
                       case "14":
                           message = "Somebody call the cops. It’s got to be illegal for "
                               + flirtTarget + " to look that good.";
                           break;
                       case "15":
                           this.reply(replyToken, new StickerMessage("11537", "52002742"));
                           break;
                       case "16":
                           this.reply(replyToken, new StickerMessage("11537", "52002737"));
                           break;
                       case "17":
                           this.reply(replyToken, new StickerMessage("11538", "51626495"));
                           break;
                       case "18":
                           this.reply(replyToken, new StickerMessage("11539", "52114119"));
                           break;
                       case "19":
                           this.reply(replyToken, new StickerMessage("11539", "52114132"));
                           break;
                       case "20":
                           message = "No! " + flirtTarget + " no, please!";
                           break;
                       case "21":
                           message = "No! Because " + flirtTarget + " is a cheater!";
                           break;
                       case "22":
                           message = "No! " + flirtTarget + " no! I rather shutdown msyself!";
                           break;
                       case "23":
                           message = "No! " + flirtTarget + " does watch too much porn!";
                           break;
                       case "24":
                           message = "I'm not programmed to flirt with " + flirtTarget;
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
            }
            case "yukobot": {
                log.info("Returns echo message {}: {}", replyToken, text);
                myrandInt = rand.nextInt(6);
                strRandom = "" + myrandInt;
                switch (strRandom) {
                       case "0":
                           message = "Where is the bot?";
                           break;
                       case "1":
                           message = "Is there a bot here?";
                           break;
                       case "2":
                           message = "A bot? Where? \uDBC0\uDC1C";
                           break;
                       case "3":
                           message = "Some bots are nice \uDBC0\uDC04";
                           break;
                       case "4":
                           message = "Shhh... there is a bot here...";
                           break;
                       case "5":
                           message = "Are you talking about me?";
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
            }
            case "bob": {
                log.info("Returns echo message {}: {}", replyToken, text);
                myrandInt = rand.nextInt(5);
                strRandom = "" + myrandInt;
                switch (strRandom) {
                       case "0":
                           message = "Who is Bob?";
                           break;
                       case "1":
                           message = "Bob! come here!";
                           break;
                       case "2":
                           message = "Bob is not here...";
                           break;
                       case "3":
                           message = "He's gone...";
                           break;
                       case "4":
                           message = "Don't call him!";
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
            }
            case "quickreply":
                this.reply(replyToken, new MessageWithQuickReplySupplier().get());
                break;
            case "yuko": {
                myrandInt = rand.nextInt(20);
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
                           message = "Yeah?";
                           break;
                       case "3":
                           message = "That's my name!";
                           break;
                       case "4":
                           message = "Why are you calling me? \uDBC0\uDC09";
                           break;
                       case "5":
                           message = "Do you need something?";
                           break;
                       case "6":
                           message = "May I help you?";
                           break;
                       case "7":
                           message = "Hey!";
                           break;
                       case "8":
                           message = "I' here!";
                           break;
                       case "9":
                           message = "\uDBC0\uDC01";
                           break;
                       case "10":
                           message = "You love my name, right? \uDBC0\uDC07";
                           break;
                       case "11":
                           message = "...";
                           break;
                       case "12":
                           message = "Uh?";
                           break;
                       case "13":
                           message = "What's up?";
                           break;
                       case "14":
                           message = "Come on! Again this?";
                           break;
                       case "15":
                           message = "Do you need something from me?";
                           break;
                       case "16":
                           message = "I know you are there!";
                           break;
                       case "17":
                           message = "Please! Not again...";
                           break;
                       case "18":
                           message = "Here!";
                           break;
                       case "19":
                           message = "Please! Be nice!";
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
             }
            case "thanksYuko":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.replyText(replyToken, "You are welcome!");
                break;
            case "hello": {
                myrandInt = rand.nextInt(10);
                log.info("Returns echo message {}: {}", replyToken, text);
                strRandom = "" + myrandInt;
                switch (strRandom) {
                       case "0":
                           this.reply(replyToken, new StickerMessage("11539", "52114114"));
                           break;
                       case "1":
                           message = "Hi there!";
                           break;
                       case "2":
                           message = "Hello!";
                           break;
                       case "3":
                           message = "Hey! what's up?";
                           break;
                       case "4":
                           message = ":)";
                           break;
                       case "5":
                           message = "I'm glad to see you again!";
                           break;
                       case "6":
                           message = "There you are...";
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
            }
            case "bye": {
                myrandInt = rand.nextInt(10);
                log.info("Returns echo message {}: {}", replyToken, text);
                strRandom = "" + myrandInt;
                switch (strRandom) {
                       case "0":
                           this.reply(replyToken, new StickerMessage("11537", "52002771"));
                           break;
                       case "1":
                           message = "Bye bye!";
                           break;
                       case "2":
                           message = "See you later!";
                           break;
                       case "3":
                           message = "Take care!";
                           break;
                       case "4":
                           message = "Seee youuu!";
                           break;
                       case "5":
                           message = "Well... Bye!";
                           break;
                       case "6":
                           message = "There you go...";
                           break;
                       case "7":
                           message = "Enjoy!";
                           break;
                       case "8":
                           message = "We'll be waiting!";
                           break;
                       default:
                           break;
                }
                this.replyText(replyToken, message);
                break;
            }
            case "questionYuko":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.reply(replyToken, new StickerMessage("11539", "52114129"));
                break;
            case "lurking":
                log.info("Returns echo message {}: {}", replyToken, text);
                this.replyText(replyToken, "Hahaha! Thats me!");
                break;
            case "testyuko":
                log.info("Returns echo message {}: {}", replyToken, text);
                String testReply = "\uDBC0\uDCA5";
                this.replyText(replyToken, testReply);
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
