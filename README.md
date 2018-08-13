# GSoC 2018 | vitrivr | vitrivr App - Vaibhav Maheshwari
As a part of GSoC 2018, I worked on vitrivr App which is an Android client for interacting with the vitrivr stack. This application is created from the scratch taking advantage of Android native features to provide an easy interface for constructing media queries. I also did some preliminary work on extending cineast functionality by adding support for 'Add Media API' which can be used to upload and extract media files by sending a HTTP request to the API.

## About vitrivr
vitrivr is an open source full-stack content-based multimedia retrieval system with focus on video. Its modular architecture makes it however easy to process different types of media as well. More information can be found on the [vitrivr website](https://vitrivr.org) or if you prefer, you can dive into [getting started guide](https://vitrivr.org/getting_started.html) right away for setting up the vitrivr stack.

## Project Links
This repository itself is major part of the project created completely by me.

The 'Add Media API' can be found in [gsoc-2018 branch of cineast](https://github.com/vitrivr/cineast/tree/gsoc-2018)  
Link to commit can be found [here](https://github.com/vitrivr/cineast/commit/e82c09c83a1fd2ce390ca3814bfd81a62d1dff54)  
Cineast API Documentation can be found [here](https://github.com/iammvaibhav/Cineast-API/wiki).

## My Contributions
* The main goal of the project was to provide better support for Android devices which [vitrivr-ng](https://github.com/vitrivr/vitrivr-ng) (Web based UI) couldn't. This app provides all the features currently present in [vitrivr-ng](https://github.com/vitrivr/vitrivr-ng) for Android.
* Spatial query interface is also implemented in the app.
* Documentation of Cineast API (both REST and WebSocket)
* Preliminary work on 'Add Media API' for cineast with UI for Android.

## See it in action

[![Demo of vitrivr App](https://image.ibb.co/c33T39/video_Youtube.png)](https://youtu.be/yYAVT22My9w "Demo of vitrivr App")

## What's left
* Making 'Add Media API' more functional, robust and secure.
* 3D Model queries currently use WebView for using three.js library's geometry to JSON conversion algorithm. Implementing the required functionality in native platform to increase efficiency.

## Future prospects
* Providing option for using cineast REST API.
* Adding neighboring segment query option.
* Adding support for Metadata & Tag lookup.

## Challenges Faced
* In the beginning, I spent a lot of time in deciding and implementing the architecture and the project structure. This was mainly because I was used to MVP architecture but for this project, I decided to use the current popular and Android's recommended MVVM architecture. Turns out, the result is worth the time spent. Using MVVM, stream data flow and orientation handling turned out quite easy and robust.
* While implementing audio queries, I was getting a errors due to wrong sample rate and channel which I figured out after a while. In the process, I dived into various intricacies of digial audio (which I later realized were not needed) which helped me learn a lot about them.
* While imlementing 3d model queries, major challenge was poor 3D library support for android which was solved by using three.js Javascript library.

## Architecture
I tried to follow the best practices for Android and maintained high standards in terms of code quality and structure so that there is no compromise in app performance and stability:
* [Model-View-ViewModel (MVVM)](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) architecture
* Use of [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) for communication between View & [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel), and [RxJava](https://github.com/ReactiveX/RxJava) for communication between Model & [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel).
* Maintained modular structure addressing seperation of concerns.
* Dependency Injection using [Dagger2](https://github.com/google/dagger).
<p align="center"><img src="https://image.ibb.co/irqN3o/architecture.png" alt="App Architecture"/></p>

* A Service interacts with a Data source (Network, SharedPreferences etc)
* A Repository provides single source of entry to multiple services

## Project Structure
The whole project is divided into five top level packages:
* **components:** Place for all shared activities (like [DrawingActivity](app/src/main/java/org/vitrivr/vitrivrapp/components/drawing/DrawingActivity.kt), [MotionDrawingActivity](app/src/main/java/org/vitrivr/vitrivrapp/components/drawing/MotionDrawingActivity.kt)), views (like [SquareCardView](https://github.com/iammvaibhav/vitrivr-App/blob/master/app/src/main/java/org/vitrivr/vitrivrapp/components/results/SquareCardView.kt), [SquareImageView](app/src/main/java/org/vitrivr/vitrivrapp/components/results/SquareImageView.kt)) and other stuff (like [EqualSpacingItemDecoration](app/src/main/java/org/vitrivr/vitrivrapp/components/results/EqualSpacingItemDecoration.kt)).
* **data:** Contains packages and classes related to retrieving data from various sources. All models, repositories and services are contained in this package. The main WebSocket query handling is done in [QueryResultsService](app/src/main/java/org/vitrivr/vitrivrapp/data/services/QueryResultsService.kt). [model](app/src/main/java/org/vitrivr/vitrivrapp/data/model) contains all the model objects for queries & results.
* **di:** Contains classes used for Dependency Injection
* **features:** Each sub-package represents a single screen or group of related screens and contains Views and ViewModels for them. Specifically whole app functionality is divided into [add media](app/src/main/java/org/vitrivr/vitrivrapp/features/addmedia), [query](app/src/main/java/org/vitrivr/vitrivrapp/features/query), [results](app/src/main/java/org/vitrivr/vitrivrapp/features/results), [result details](app/src/main/java/org/vitrivr/vitrivrapp/features/resultdetails) & [settings](app/src/main/java/org/vitrivr/vitrivrapp/features/settings) feature.
* **utils:** Utility classes (like [Extensions](app/src/main/java/org/vitrivr/vitrivrapp/utils/Extensions.kt) containing extension functions)

## How does it look?

  | <img src="https://image.ibb.co/kUM4G8/Screenshot_20180730_152727.png" width="230px"/>  | <img src="https://image.ibb.co/gi59io/Screenshot_20180730_153658.png" width="230px"/> | <img src="https://image.ibb.co/bTmh3o/Screenshot_20180730_153717.png" width="230px"/>
  |:---:|:---:|:---:|
  | <img src="https://image.ibb.co/i5DvOo/Screenshot_20180730_153741.png" width="230px"/> | <img src="https://image.ibb.co/nesN3o/Screenshot_20180730_153807.png" width="230px"/> | <img src="https://image.ibb.co/bTqLpT/Screenshot_20180730_153817.png" width="230px"/>
  | <img src="https://image.ibb.co/dFA9io/Screenshot_20180730_153822.png" width="230px"/> | <img src="https://image.ibb.co/gwR4G8/Screenshot_20180730_154124.png" width="230px"/> | <img src="https://image.ibb.co/htmh3o/Screenshot_20180730_154148.png" width="230px"/>
