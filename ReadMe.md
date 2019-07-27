# Video Ad Remover
Multimedia Systems Design Project


There is an increased amount of video and audio content broadcast and streamed everywhere today. Such content needs to be frequently analyzed for a variety of reasons and applications – such as searching, indexing, summarizing, etc. One general area is in modifying content is to remove/replace specific parts of frames or even a number of frames altogether. Let’s consider removal of frames from video & audio content. The motivating question that you will need to think about in this project may be described by - how do you automatically analyze video/audio and remove frames that correspond to a specific description. </br>
General examples of this type may include: </br>
• You want to watch a video recording of a sports game, but you want to remove all the non-interesting areas and see only the sections that have good plays and goals scored.</br>
• On the audio side, you must have seen “bleep censoring” or “bleeping” which is defined as the replacement of profane words, or even classified information with a beep sound – usually a 1000Hz tone</br>
• You want to quickly process a long, mostly boring surveillance video and cut out the uninteresting parts so that only desirable sections of “interesting events” can be highlighted.</br>
• You want remove all video frames from a recorded video that shows a specific person or a copyrighted object in there.</br></br>

In this project we design an algorithm to automatically remove advertisements from a video (and corresponding audio) which is interspersed with advertisements. Furthermore, we use opencv to detect specific brand images in videos and if present, replace the original advertisement with a corresponding topical advertisement.</br>
Functions of program:</br>
1. Video player that is able to synchronize the video and audio rendering based on the video frame rate and the audio sampling rate. Play, pause and stop are all implemented.</br>
2. Takes input of video/audio stream with advertisement sections and creates a new video/audio stream with the advertisements removed.</br>
3. Analyzes the video frames to see if any known brands exist, and if so, inserts the right advertisement at the appropriate original place. All removements and replacement frames are inserted seamlessly.</br></br>

Techniques used:</br>
• Histogram comparing between video frames/video scenes to detect large flunctuations in pixel information(large flunctuations in short timeframes could be ads)</br>
• OpenCV(SIFT image detection algorithm for dectecting brand images within video)</br></br>
Program Input: </br>
• Video (and corresponding audio) files with advertisements in them.(rgb + .wav) format</br>
• Brand image files to be detected in the input videos</br>
• Brand advertisements in the same format(.rgb + .wav) to be inserted in the video</br>

Anatomy of a video:</br>
</br>![alt text](https://raw.githubusercontent.com/samuel40791765/VideoAdRemover/master/projectimages/pic.png) </br>
• Frame: a single still image from a video, eg NTSC - 30 frames/second, film – 24 frames/second</br>
• Shot: sequence of frames recorded in a single camera operation</br>
• Sequence or Scenes: collection of shots forming a semantic unit which conceptually may be shot at a single time and place</br>





