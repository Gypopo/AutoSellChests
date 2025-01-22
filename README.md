<p align="center">
  <img src="https://imgur.com/QPOrULJ.png" width="350" title="AutoSellChests icon">
  <br>
  <img src="https://img.shields.io/discord/555732704310853682?color=7289DA&label=Discord&logo=discord&logoColor=7289DA&link=https://discord.com/invite/nPyuB4F"/>
  <img src="https://img.shields.io/spiget/downloads/103060?label=Downloads&color=FF9E0F" alt="Spigot downloads&link=https://www.spigotmc.org/resources/autosellchests.103060/">
  <img src="https://img.shields.io/spiget/version/103060?color=00CA&label=Version&link=https://www.spigotmc.org/resources/autosellchests.103060/" alt="Version&link=https://www.spigotmc.org/resources/autosellchests.103060/">
</p>

# AutoSellChests
A simple addon for EconomyShopGUI which adds automatically selling chests to your server.

## Introduction
AutoSellChests allows you to have a completely automated farm at almost to no performance cost of your server.

This addon adds chests to your server which sell their content every x interval which is specified inside the config.
The items will then be sold to the shop of [EconomyShopGUI](https://www.spigotmc.org/resources/economyshopgui.69927/update?update=419331) using the API.

With that sayd, this plugin deppends on EconomyShopGUI or EconomyShopGUI Premium but doesn't require a economy plugin because it will use the one used within the shop.

See more info here about how to use the API of EconomyShopGUI: https://github.com/Gypopo/EconomyShopGUI-API

This is a lightweight plugin thanks to how the chests are sold, the chests get processed each one at a time spread over the complete interval instead of selling all chests at a single time which could cause major lag spikes.

There is a default English language file embeded which contains all messages from the plugin which then can be translated to your likings.

This plugin is compatible with minecraft version 1.16-1.21.

## Downloads:
- [SpigotMC](https://www.spigotmc.org/resources/autosellchests.103060/)
- [Polymart](https://polymart.org/resource/autosellchests.2583)
- [Hangar](https://hangar.papermc.io/GPPlugins/AutoSellChests)

# Contributing to the project
All contributions should follow our [code of conduct](CODE_OF_CONDUCT.md) guidelines.

Since this is a open source project there are a few possibility's how you can help make this project better.
## ❓ Support/Feature requests
We will happily answer your questions and look at feature requests you may have in our [discord server](https://discord.com/invite/nPyuB4F). 

Note that we get alot of questions which can be answered by simply reading the wiki/plugin pages so first make sure that your question cannot be answered using the info provided before seeking furthur support.

## 🐛 Reporting bugs
If you stumble across a bug you may use the issues tab to create a new bug report. But please make sure the issue doesn't exists already before making duplicates.

Bug reports can also be made inside our [discord server](https://discord.com/invite/nPyuB4F) under the category of the plugin.

## 🛠️ Building
AutoSellChests uses `Maven` for compiling, to build the project you will need the following:
- A recent clone of the project
- [Maven](https://maven.apache.org/download.cgi) (Unless you have an IDE with it build in)
- JDK 16 or newer
- Git

The following commands can be used to build the project:
```
git clone https://github.com/Gypopo/AutoSellChests.git
cd AutoSellChests/
mvn clean install
```
If you have an error which says that EconomyShopGUI isn't found, you need to make sure that you configure the path inside the [`pom.xml`](https://github.com/Gypopo/AutoSellChests/blob/main/pom.xml) file leading to the jar of EconomyShopGUI.

## ✏️ Contributing:
Getting started:
- Start by cloning this repository into your own workspace or make sure your existing clone is up to date with the `Main` branch
- Open the project in your favorite IDE and make your changes
- When you are done, make a pull request directly into the `Main` branch and make sure you have a clear description of your changes and why you made them

Here some simple rules to keep in mind when contributing:
- Make sure you've tested the changes in your pull request
- When fixing a bug using a pull request, please make sure you create a issue on the issue tracker first
- Pull requests should have a clear description of what they do and why you submitted it
- Calls to EconomyShopGUI.getInstance() should be prevented unless necessary

This plugin makes use of bStats to collect anonymous data such as how many servers, players, ... are using the plugin.

See more info at [bStats](https://bstats.org/)
![bStats](https://bstats.org/signatures/bukkit/AutoSellChests.svg)
