import wx
import cv2
from Video import Video


class MainWindow(wx.Frame):
    def __init__(self, parent, capture):
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
        cap = self.ShowCapture(self, videoframe, capture)
        videoboxsizer.Add(videoframe, 0)
        mainSizer.Add(videoboxsizer, 0)

        # buttons
        sbox = wx.StaticBox(self, label='buttons:')
        sboxSizer = wx.StaticBoxSizer(sbox, wx.VERTICAL)
        hbox = wx.BoxSizer(wx.HORIZONTAL)
        # pause button
        bmp = wx.Bitmap("pause.jpg", wx.BITMAP_TYPE_JPEG)
        self.bmpbtn = wx.BitmapButton(panel, id=wx.ID_ANY, bitmap=bmp,
                                      size=(bmp.GetWidth(), bmp.GetHeight()))
        hbox.Add(self.bmpbtn, 0, wx.ALIGN_CENTER)
        self.bmpbtn.Bind(wx.EVT_BUTTON, self.pause_play)
        self.bmpbtn.SetLabel("Pause")

        # stop button
        bmp1 = wx.Bitmap("stop.png", wx.BITMAP_TYPE_PNG)
        self.bmpbtn1 = wx.BitmapButton(panel, id=wx.ID_ANY, bitmap=bmp1,
                                       size=(bmp.GetWidth(), bmp.GetHeight()))
        hbox.Add(self.bmpbtn1, 0, wx.ALIGN_CENTER)
        self.bmpbtn1.Bind(wx.EVT_BUTTON, self.OnClicked)
        self.bmpbtn1.SetLabel("Stop")

        sboxSizer.Add(hbox, 0, wx.LEFT)
        mainSizer.Add(sboxSizer, 0, wx.CENTER)

        self.Centre()
        self.Show()
        panel.SetSizerAndFit(mainSizer)

    def OnClicked(self, event):
        btn = event.GetEventObject().GetLabel()
        print("test1")

    def pause_play(self, event):
        if self.play:
            btn = event.GetEventObject()
            btn.SetBitmap(wx.Bitmap("play.png", wx.BITMAP_TYPE_PNG))
            self.play = False
            print("Pause")
        else:
            btn = event.GetEventObject()
            btn.SetBitmap(wx.Bitmap("pause.jpg", wx.BITMAP_TYPE_JPEG))
            self.play = True
            print("Play")

    def stop(self, event):
        btn = event.GetEventObject().GetLabel()
        self.stop = True
        print("Stop")

    # inner class of main window to render video
    class ShowCapture(wx.Panel):

        def __init__(self, outer_instance, parent, capture, fps=24):
            wx.Panel.__init__(self, parent, wx.ID_ANY, (0, 0),
                              (capture.get(cv2.CAP_PROP_FRAME_WIDTH), capture.get(cv2.CAP_PROP_FRAME_HEIGHT)))
            self.capture = capture
            self.outer_instance = outer_instance

            ret, frame = self.capture.read()

            height, width = frame.shape[:2]

            parent.SetSize((width, height))

            frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGBA)

            self.bmp = wx.BitmapFromBuffer(width, height, frame)

            self.timer = wx.Timer(self)
            self.timer.Start(1000./fps)

            self.Bind(wx.EVT_PAINT, self.OnPaint)
            self.Bind(wx.EVT_TIMER, self.NextFrame)

        def OnPaint(self, evt):
            dc = wx.BufferedPaintDC(self)
            dc.DrawBitmap(self.bmp, 0, 0)

        def NextFrame(self, event):
            if self.outer_instance.play:
                ret, frame = self.capture.read()
                if ret:
                    frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                    self.bmp.CopyFromBuffer(frame)
                    self.Refresh()
