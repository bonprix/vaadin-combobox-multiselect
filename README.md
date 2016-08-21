# ComboBoxMultiselect Add-on for Vaadin 7

The ComboBoxMultiselect component is a client-side Widget. As example was used the ComboBox from Vaadin and the VFilterSelect from Vaadin.

![screenshot](assets/screenshot1.png)

### Features:
- multiselect with checkbox
- clear selection button
- ordering moves selected always to top

## Online demo
http://bonprix.jelastic.servint.net/vaadin-combobox-multiselect-demo/

## Usage

### Maven

```xml
<dependency>
    <groupId>org.vaadin.addons</groupId>
	<artifactId>vaadin-combobox-multiselect</artifactId>
	<version>1.1.14</version>
</dependency>

<repository>
   <id>vaadin-addons</id>
   <url>http://maven.vaadin.com/vaadin-addons</url>
</repository>
```

No widgetset required.

## Download release

Official releases of this add-on are available at Vaadin Directory. For Maven instructions, download and reviews, go to http://vaadin.com/addon/vaadin-combobox-multiselect

## Building and running demo

git clone https://github.com/bonprix/vaadin-combobox-multiselect
mvn clean install
cd demo
mvn jetty:run

To see the demo, navigate to http://localhost:8080/
 
## Release notes

### Version 1.1.0
- client-side component

## Known issues

- please report issues and help us to make this even better ;)

## Roadmap

This component is developed as a part of a bonprix project with no public roadmap or any guarantees of upcoming releases. That said, the following features are planned for upcoming releases:
- use scss

## Issue tracking

The issues for this add-on are tracked on its github.com page. All bug reports and feature requests are appreciated. 

## Contributions

Contributions are welcome, but there are no guarantees that they are accepted as such. Process for contributing is the following:
- Fork this project
- Create an issue to this project about the contribution (bug or feature) if there is no such issue about it already. Try to keep the scope minimal.
- Develop and test the fix or functionality carefully. Only include minimum amount of code needed to fix the issue.
- Refer to the fixed issue in commit
- Send a pull request for the original project
- Comment on the original issue that you have implemented a fix for it

## License & Author

Add-on is distributed under MIT License. For license terms, see LICENSE.txt.

vaadin-combobox-multiselect is written by members of Bonprix Handelsgesellschaft mbh:
- Thorben von Hacht (https://github.com/thorbenvh8)

# Developer Guide

## Getting started

Here is a simple example on how to try out the add-on component:

```java

// Initialize a list with items
List<NamedObject> list = new ArrayList<NamedObject>();
NamedObject vaadin = new NamedObject(2L, "Vaadin”);
list.add(new NamedObject(1L, "Java"));
list.add(vaadin);
list.add(new NamedObject(3L, "Bonprix"));
list.add(new NamedObject(4L, "Addon"));

// Initialize the ComboBoxMultiselect
final ComboBoxMultiselect comboBoxMultiselect = new ComboBoxMultiselect();
comboBoxMultiselect.setInputPrompt("Type here");
comboBoxMultiselect.setCaption("ComboBoxMultiselect");
comboBoxMultiselect.addItems(list);
comboBoxMultiselect.select(vaadin);

```

For a more comprehensive example, see src/test/java/org/vaadin/template/demo/DemoUI.java
