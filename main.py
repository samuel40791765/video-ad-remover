import numpy as np
import wx
import cv2
from scipy.io import wavfile
from MainWindow import MainWindow


video = cv2.VideoCapture('dataset/Videos/data_test1_cmp.avi')
fs, data = wavfile.read('dataset/Videos/data_test1.wav')
app = wx.App(False)
panel = MainWindow(None, video)
app.MainLoop()

