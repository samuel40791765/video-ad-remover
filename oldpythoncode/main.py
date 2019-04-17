import numpy as np
import wx
import cv2
import soundfile as sf
from MainWindow import MainWindow

videofile = 'dataset/Videos/data_test1.rgb'
audiofile = 'dataset/Videos/data_test1.wav'
video = cv2.VideoCapture(videofile)
# fs, data = wavfile.read('dataset/Videos/data_test1.wav')

sound, fs = sf.read(audiofile, dtype='float32')
app = wx.App(False)
panel = MainWindow(None, videofile, audiofile)
app.MainLoop()

