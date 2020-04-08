/*
 * Copyright 2018 LINE Corporation
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

import static java.util.Arrays.asList;

import java.util.function.Supplier;

import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.flex.component.Box;
import com.linecorp.bot.model.message.flex.component.Image;
import com.linecorp.bot.model.message.flex.component.Image.ImageAspectMode;
import com.linecorp.bot.model.message.flex.component.Image.ImageSize;
import com.linecorp.bot.model.message.flex.component.Separator;
import com.linecorp.bot.model.message.flex.component.Spacer;
import com.linecorp.bot.model.message.flex.component.Text;
import com.linecorp.bot.model.message.flex.component.Text.TextWeight;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.unit.FlexAlign;
import com.linecorp.bot.model.message.flex.unit.FlexFontSize;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.flex.unit.FlexMarginSize;

public class WeatherFlexMessageSupplier implements Supplier<FlexMessage> {
    @Override
    public FlexMessage get() {
        final Image heroBlock =
                Image.builder()
                     .url("https://openweathermap.org/img/wn/02d@2x.png")
                     .size(ImageSize.MD)
                     .aspectMode(ImageAspectMode.Fit)
                     .build();
        final Box bodyBlock = createBodyBlock();
        final Box footerBlock = createFooterBlock();
        final Bubble bubble =
                Bubble.builder()
                      .hero(heroBlock)
                      .body(bodyBlock)
                      .footer(footerBlock)
                      .build();

        return new FlexMessage("Weather results", bubble);
    }

    private Box createFooterBlock() {
        final Spacer spacer = Spacer.builder().size(FlexMarginSize.SM).build();
        final Separator separator = Separator.builder().build();
        final Text weatherSupplier =
                Text.builder()
                    .text("Courtesy of OpenWeather®")
                    .align(FlexAlign.CENTER)
                    .weight(TextWeight.REGULAR)
                    .size(FlexFontSize.XS)
                    .build();

        return Box.builder()
                  .layout(FlexLayout.VERTICAL)
                  .spacing(FlexMarginSize.SM)
                  .contents(asList(spacer, separator, weatherSupplier))
                  .build();
    }

    private Box createBodyBlock() {
        KitchenSinkController weather = new KitchenSinkController();
        String location = weather.testwreportLocation();
        final Text title = Text
                   .builder()
                   .text(location)
                   .weight(TextWeight.BOLD)
                   .size(FlexFontSize.LG)
                   .build();

        final Box info = createInfoBox();

        return Box.builder()
                  .layout(FlexLayout.VERTICAL)
                  .contents(asList(title, info))
                  .build();
    }

    private Box createInfoBox() {
        final Box conditions = Box
                .builder()
                .layout(FlexLayout.BASELINE)
                .spacing(FlexMarginSize.SM)
                .contents(asList(
                        Text.builder()
                            .text("Conditions")
                            .color("#aaaaaa")
                            .size(FlexFontSize.SM)
                            .flex(2)
                            .build(),
                        Text.builder()
                            .text("Few clouds")
                            .wrap(true)
                            .color("#666666")
                            .size(FlexFontSize.SM)
                            .flex(4)
                            .build()
                ))
                .build();
        final Box currtemperature =
                Box.builder()
                   .layout(FlexLayout.BASELINE)
                   .spacing(FlexMarginSize.SM)
                   .contents(asList(
                           Text.builder()
                               .text("Temperature")
                               .color("#aaaaaa")
                               .size(FlexFontSize.SM)
                               .flex(2)
                               .build(),
                           Text.builder()
                               .text("25°C")
                               .wrap(true)
                               .color("#666666")
                               .size(FlexFontSize.SM)
                               .flex(4)
                               .build()
                   ))
                   .build();
        final Box mintemperature =
                Box.builder()
                   .layout(FlexLayout.BASELINE)
                   .spacing(FlexMarginSize.SM)
                   .contents(asList(
                           Text.builder()
                               .text("- Minimum")
                               .color("#aaaaaa")
                               .size(FlexFontSize.SM)
                               .flex(2)
                               .build(),
                           Text.builder()
                               .text("22°C")
                               .wrap(true)
                               .color("#666666")
                               .size(FlexFontSize.SM)
                               .flex(4)
                               .build()
                   ))
                   .build();
        final Box maxtemperature =
                Box.builder()
                   .layout(FlexLayout.BASELINE)
                   .spacing(FlexMarginSize.SM)
                   .contents(asList(
                           Text.builder()
                               .text("- Maximum")
                               .color("#aaaaaa")
                               .size(FlexFontSize.SM)
                               .flex(2)
                               .build(),
                           Text.builder()
                               .text("29°C")
                               .wrap(true)
                               .color("#666666")
                               .size(FlexFontSize.SM)
                               .flex(4)
                               .build()
                   ))
                   .build();
        final Box humidity =
                Box.builder()
                   .layout(FlexLayout.BASELINE)
                   .spacing(FlexMarginSize.SM)
                   .contents(asList(
                           Text.builder()
                               .text("Humidity")
                               .color("#aaaaaa")
                               .size(FlexFontSize.SM)
                               .flex(2)
                               .build(),
                           Text.builder()
                               .text("78%")
                               .wrap(true)
                               .color("#666666")
                               .size(FlexFontSize.SM)
                               .flex(4)
                               .build()
                   ))
                   .build();

        return Box.builder()
                  .layout(FlexLayout.VERTICAL)
                  .margin(FlexMarginSize.LG)
                  .spacing(FlexMarginSize.SM)
                  .contents(asList(conditions, currtemperature, maxtemperature, mintemperature, humidity))
                  .build();
    }
}
