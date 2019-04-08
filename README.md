[![Android Arsenal]( https://img.shields.io/badge/Android%20Arsenal-Sequence%20Layout-green.svg?style=flat )]( https://android-arsenal.com/details/1/7609 )
# Sequence Layout
SequenceLayout is a new solution to layout problem. Taken account the strenghts and weaknesses of ConstraintLayout, this new layout is much more flexible and also very much simpler to understand and define.

While being more light weight than ConstraintLayout, SequenceLayout makes it possible to support a wider range of screen sizes.

## Sample
This sample layout is defined by about 170 lines of XML. It is consisted of 22 views which took about 200 lines to define.
The following animations display the extend of the flexibily available.

<img src="https://github.com/yasharpm/SequenceLayout/raw/master/vertical.gif" width="260px"/><img src="https://github.com/yasharpm/SequenceLayout/raw/master/horizontal.gif" width="260px"/><img src="https://github.com/yasharpm/SequenceLayout/raw/master/both.gif" width="260px"/>

## Usage

Add the dependency:
```Groovy
dependencies {
	implementation 'com.yashoid:sequencelayout:1.1.0'
}
```

## How to use this library
SequenceLayout is based on two core consepts:
- `Span`: Is an extend definition. In contrast to other layouts, margins and spaces are treated as sizing entities same as view dimensions. Each Span has a `size`, optional `min` and `max` for size limits, optional `id` to assign it to a view's horizontal or vertical extend, and an optional `visibilityElement` which means the size should be resolved to zero if the visibility element's visibility is set to `View.GONE`.
- Sequence: Is a sequence of spans that resolves the extends (Spans) to actual positions on the screen. For `Vertical` sequences the first span is positioned from 0 to the span's resolved size. The next span's position starts after the previous span end position. Same is valid for `Horizontal` sequences from left to right (from x equal to zero).

Hint: Sequences also have optional `start` and `end` properties that are defined relative to other views or the container. The default value for `start` is `0@` meaning zero percent of the parent and default value for `end` is subsequently `100@`.

### Hello SequenceLayout
layout/activity_main.xml
```XML
<?xml version="1.0" encoding="utf-8"?>
<com.yashoid.sequencelayout.SequenceLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"

    app:pgSize="360"
    app:sequences="@xml/sequences_main">

    <View
        android:id="@+id/view"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"

        android:background="#abc34f"/>
 
</com.yashoid.sequencelayout.SequenceLayout>
```
`pgSize` is an adaptive size that allows you to define your `Span` sizes relative to available width using `pg` unit.
`sequences` refers to an XML file that defines the positioning of sequence layout's children. Note that `android:layout_width` and `android:layout_height` are virtually ignored.

xml/sequences_main
```XML
<?xml version="1.0" encoding="utf-8"?>
<Sequences>
  
  <Horizontal>
    <Span size="1w"/>
    <Span id="@id/view" size="40pg"/>
    <Span size="1w"/>
  </Horizontal>
                
  <Vertical>
    <Span size="1w"/>
    <Span id="@id/view" size="40pg"/>
    <Span size="1w"/>
  </Vertical>
  
</Sequences>
```
This resolves to putting the view in the center of the screen as a square of `40pg` size. Which means `width = 40 / 360 * total_width`.

### Span sizes cheat sheet
- `%` Size relative to sequence's total size. Examples: `30%`, `100%`, `-10.5%`, `150%`
- `px` Size in pixels. Examples: `12px`, `60px`, `-8.5px`
- `mm` Size in millimeters. Examples: `10mm`, `1mm`, `0.85mm`
- `wrap` Wrapping size. Specific to use for views. Is equivalent for the known `wrap_content` behaviour.
- `%[view_id]` Size percentage relative to another view's size. Examples: `30%view`, `100%text_title`, `-10%image_profile`
- `align@[view_id]` Set the size so that the Span would end at the start of the given id's view. Examples: `align@text_title`, `align@image_profile`
- `pg` Relative to the `pgSize` defined for the SequenceLayout. Meant to be the main sizing unit and to replace `dp` sizes. This allows you to define your layout solely by following the sizes that are given to you by the designers. Examples: `12pg`, `1.5pg`, `-4pg`
- `w` Weighted size inside of each sequence's scope. After all other sizes have resolved. The remaining space is divided between the weighed spans relative to their weight. If the SequenceLayout is set to horizontal or vertical wrapping, all weighted sizes will be resolved to zero. Examples: `1w`, `20w`, `4.5w`, `-3w`
- `@MAX(span_size,...)` Resolves to the maximum value of the given span sizes. Note that weighted and aligned sizes are not valid. Examples: `@MAX(48pg,100%text_title,25%image_profile)`, `@MAX(100%view,20%)`
