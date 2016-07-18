#coding=utf-8
'''
copyright 2016 wanghongyu. 
The project page：https://github.com/hardman/FlashAnimationToMobile
My blog page: http://blog.csdn.net/hard_man/
'''
import os
import sys
import json
import struct

def toBinBool(data):
    return struct.pack("?",data)

def toBinUShort(data):
    return struct.pack("H",data)

def toBinStr(data):
    dataLen = len(data)
    binUShort = toBinUShort(dataLen)

    return binUShort + data.encode("utf-8")

def toBinFloat(data):
    return struct.pack("f",data)

def toBinUChar(data):
    return struct.pack("B",data)

def flaJsonToBin(filePath,outputFilePath):
    f = open(filePath) 
    jsonData = json.loads(f.read())
    f.close()

    f = open(outputFilePath,'wb');

    # animName
    #f.write(toBinStr(jsonData["name"]))
    f.write(toBinUShort(jsonData["frameRate"]))

    texDict = {}
    texIndex = 0
    # textures
    f.write(toBinUShort(jsonData["texNum"]))
    for texName in jsonData["textures"]:
        f.write(toBinStr(texName))
        texDict[texName] = texIndex
        texIndex = texIndex + 1

    # anims
    f.write(toBinUShort(jsonData["animNum"]))
    for anim in jsonData["anims"]:
        f.write(toBinStr(anim["animName"]))
        f.write(toBinUShort(anim['frameMaxNum']))
        f.write(toBinUShort(anim["layerNum"]))

        for layer in anim["layers"]:
            f.write(toBinUShort(layer['keyFrameNum']))

            for frame in layer["frames"]:
                isEmpty = frame["isEmpty"]
                f.write(toBinBool(isEmpty))
                f.write(toBinUShort(frame['frameIndex']))

                if not isEmpty:
                    f.write(toBinUShort(frame["duration"]))
                    f.write(toBinBool(frame["isTween"]))
                    f.write(toBinUShort(texDict[frame["texName"]]))
                    f.write(toBinStr(frame["mark"]))
                    f.write(toBinUChar(frame["alpha"]))
                    f.write(toBinUChar(frame["color"]["r"]))
                    f.write(toBinUChar(frame["color"]["g"]))
                    f.write(toBinUChar(frame["color"]["b"]))
                    f.write(toBinUChar(frame["color"]["a"]))
                    f.write(toBinFloat(frame["scaleX"]))
                    f.write(toBinFloat(frame["scaleY"]))
                    f.write(toBinFloat(frame["skewX"]))
                    f.write(toBinFloat(frame["skewY"]))
                    f.write(toBinFloat(frame["x"]))
                    f.write(toBinFloat(frame["y"]))
    f.close()

if len(sys.argv) != 3:
    print("usage: python [xx/xx/xxxx.flajson] [xx/xx/xxxx.flabin]")
    sys.exit()

flaJsonToBin(sys.argv[1], sys.argv[2])
