import numpy as np
import wx
import cv2
import sounddevice as sd
import pygame


class MainWindow(wx.Frame):
    def __init__(self, parent, capture, sounddata):
        super(MainWindow, self).__init__(parent, title="Ad Remover", size=(600, 600))
        panel = wx.Panel(self)
        self.play = True
        self.stop = False
        # wx.Panel.__init__(self, parent)
        mainSizer = wx.BoxSizer(wx.VERTICAL)

        # video
        videowarper = wx.StaticBox(self, label="Video")
        videoboxsizer = wx.StaticBoxSizer(videowarper, wx.VERTICAL)
        videoframe = wx.Panel(self, -1)
        cap = self.ShowCapture(self, videoframe, capture, sounddata)
        videoboxsizer.Add(videoframe, 0)
        mainSizer.Add(videoboxsizer, 0)

        # buttons
        sbox = wx.StaticBox(self)
        sboxSizer = wx.StaticBoxSizer(sbox, wx.VERTICAL)
        hbox = wx.BoxSizer(wx.HORIZONTAL)
        # pause button
        bmp = wx.Image("pause.jpg", wx.BITMAP_TYPE_ANY).Rescale(
            width=50, height=40, quality=wx.IMAGE_QUALITY_HIGH).ConvertToBitmap()
        self.bmpbtn = wx.BitmapButton(panel, id=wx.ID_ANY, size=(150, 50))
        hbox.Add(self.bmpbtn, 0, wx.ALIGN_CENTER)
        self.bmpbtn.Bind(wx.EVT_BUTTON, self.pause_play)
        self.bmpbtn.SetLabel("Pause")
        self.bmpbtn.SetBitmap(bmp)
        self.bmpbtn.SetBitmapPosition(wx.TOP)

        # stop button
        bmp1 = wx.Image("stop.png", wx.BITMAP_TYPE_ANY).Rescale(
            width=50, height=40, quality=wx.IMAGE_QUALITY_HIGH).ConvertToBitmap()
        self.bmpbtn1 = wx.BitmapButton(panel, id=wx.ID_ANY, size=(150, 50))
        hbox.Add(self.bmpbtn1, 0, wx.ALIGN_CENTER)
        self.bmpbtn1.Bind(wx.EVT_BUTTON, self.stop_video)
        self.bmpbtn1.SetLabel("Stop")
        self.bmpbtn1.SetBitmap(bmp1)
        self.bmpbtn1.SetBitmapPosition(wx.TOP)

        sboxSizer.Add(hbox, 0, wx.LEFT)
        mainSizer.Add(sboxSizer, 0, wx.CENTER)

        self.Centre()
        self.Show()
        panel.SetSizerAndFit(mainSizer)

    def pause_play(self, event):
        if self.play:
            btn = event.GetEventObject()
            btn.SetBitmap(wx.Image("play.png", wx.BITMAP_TYPE_ANY).Rescale(
                width=50, height=40, quality=wx.IMAGE_QUALITY_HIGH).ConvertToBitmap())
            btn.SetBitmapPosition(wx.TOP)
            self.play = False
            print("Pause")
        else:
            btn = event.GetEventObject()
            btn.SetBitmap(wx.Image("pause.jpg", wx.BITMAP_TYPE_ANY).Scale(
                width=50, height=40, quality=wx.IMAGE_QUALITY_HIGH).ConvertToBitmap())
            btn.SetBitmapPosition(wx.TOP)
            self.play = True
            print("Play")

    def stop_video(self, event):
        btn = event.GetEventObject().GetLabel()
        self.stop = True
        print("Stop")

    # inner class of main window to render video
    class ShowCapture(wx.Panel):

        def __init__(self, outer_instance, parent, capture, audio, fps=30):
            height, width = 270, 480
            wx.Panel.__init__(self, parent, wx.ID_ANY, (0, 0),
                              (width, height))
            self.video_stream = open(capture, "rb")
            self.outer_instance = outer_instance
            pygame.mixer.init(frequency=48000, buffer=4096)

            parent.SetSize((width, height))

            first_frame = self.video_stream.read(480*270*3)
            nparr = np.fromstring(first_frame)
            img_np = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            frame = cv2.cvtColor(img_np, cv2.COLOR_RGB2RGBA)

            self.bmp = wx.Bitmap.FromBuffer(width, height, frame)

            self.timer = wx.Timer(self)
            self.timer.Start(1000./fps)
            pygame.mixer.music.load(audio)
            pygame.mixer.music.play(1)
            self.audio_paused = False
            self.Bind(wx.EVT_PAINT, self.OnPaint)
            self.Bind(wx.EVT_TIMER, self.NextFrame)

        def OnPaint(self, evt):
            dc = wx.BufferedPaintDC(self)
            dc.DrawBitmap(self.bmp, 0, 0)

        def NextFrame(self, event):
            if self.outer_instance.play:
                frame = self.video_stream.read(480 * 270 * 3)
                nparr = np.fromstring(frame, np.uint8)
                img_np = cv2.imdecode(nparr, cv2.CV_LOAD_IMAGE_COLOR)
                frame = cv2.cvtColor(frame, cv2.COLOR_RGB2RGBA)
                self.bmp.CopyFromBuffer(frame)
                self.Refresh()
                if self.audio_paused:
                    pygame.mixer.music.unpause()
                    self.audio_paused = False
            else:
                pygame.mixer.music.pause()
                self.audio_paused = True
