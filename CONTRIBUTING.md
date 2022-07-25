# Contributing to the project
Since this is a open source project there are a few possibility's how you can help make this project better.

## ✔️ Feature requests
You may have ideas/features which make the project even better, we will happily take suggestions for the plugin in our [discord(https://discord.com/invite/nPyuB4F).

## 🐛 Reporting bugs
If you stumble across a bug you may use the issues tab to create a new bug report.

Bug reports can also be made inside our [discord server](https://discord.com/invite/nPyuB4F) under the category of the plugin.

## 🛠️ Building
AutoSellChests uses `Maven` for compiling, to build the project you will need the following:
- A copy of the free version of EconomyShopGUI as dependancy
- A recent clone of the project
- [Maven](https://maven.apache.org/download.cgi) (Unless you have an IDE with it build in)
- JDK 8 or newer
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