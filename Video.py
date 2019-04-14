import cv2


class Video:
    def __init__(self, video_capture):
        self.frames = []
        ret, frame = video_capture.read()
        while ret:
            self.frames.append(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))


