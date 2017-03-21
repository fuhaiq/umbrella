(* ::Package:: *)

(* :Title: UmbrellaOther *)

(* :Author: fuhaiq@gmail.com*)

BeginPackage["UmbrellaOther`", {"JLink`"}]

UmbrellaOther::usage = "Some interesting functions for mmascript.com";

Begin["`Private`"]

(* encode *)

UmbrellaOther`code = <|"a" -> ".-", "b" -> "-...", "c" -> "-.-.", "d" -> "-..", 
  "e" -> ".", "f" -> "..-.", "g" -> "--.", "h" -> "....", "i" -> "..",
   "j" -> ".---", "k" -> "-.-", "l" -> ".-..", "m" -> "--", 
  "n" -> "-.", "o" -> "---", "p" -> ".--.", "q" -> "--.-", 
  "r" -> ".-.", "s" -> "...", "t" -> "-", "u" -> "..-", "v" -> "...-",
   "w" -> ".--", "x" -> "-..-", "y" -> "-.--", "z" -> "--..", 
  "1" -> ".----", "2" -> "..---", "3" -> "...--", "4" -> "....-", 
  "5" -> ".....", "6" -> "-....", "7" -> "--...", "8" -> "---..", 
  "9" -> "----.", "0" -> "-----", "." -> ".-.-.-", "," -> "--..--", 
  "!" -> "-.-.--", "?" -> "..--.."|>
UmbrellaOther`withgaps = Map[StringRiffle[Characters[#], "_"] &, code];
UmbrellaOther`withpauses = Map[StringInsert[#, "___", -1] &, withgaps];
UmbrellaOther`withspace = AssociateTo[withpauses, " " -> "_______"];
UmbrellaOther`replacements = 
  Map[StringReplace[#, {"-" -> "111", "." -> "1", "_" -> "0"}] &, 
   withspace];
   
UmbrellaOther`createMorseSignal[s_String, t_] := 
 Module[{events = 
    Characters[StringReplace[ToLowerCase[s], Normal@replacements]], 
   ts, amps},
  ts = TimeSeries[ToExpression@events, {0, (Length[events] - 1)*t, t}];
  amps = AudioGenerator[ts, SampleRate -> 1000]; 
  AudioGenerator[{"Sin", 800}, Duration@amps, 
    SampleRate -> 8000] amps
  ]
  
UmbrellaOther`encodeMorse[s_String] :=
 Module[{morse,uuid},
	morse = createMorseSignal[s, .07];
	uuid = CreateUUID[];
	Export["E:/kernel/morse/" <> uuid <> ".wav", morse];
	Print["3c8ddae3-c600-4c01-92a4-2aeae7ce79f5" <> uuid]
 ]
 
 
(* decode *)

UmbrellaOther`inversecode = AssociationThread[Values[code], Keys[code]]

UmbrellaOther`decodeMorseSignal[audio_] := 
 Module[{rms, rounded, crossings, transients, shifted, dit},
  rms = AudioLocalMeasurements[audio, "RMSAmplitude", 
    PartitionGranularity -> {.01, .002}];
  rounded = Round[rms/Max@rms];
  crossings = 
   TimeSeriesInsert[
    TimeSeries[
     CrossingDetect[rounded["Values"] - .5, 
      CornerNeighbors -> True], {rounded["Times"]}], {0, 1}];
  transients = TimeSeries@Select[Normal@crossings, #[[2]] == 1 &];
  shifted = TimeSeriesShift[transients, -transients["FirstTime"]];
  dit = MinimumTimeIncrement[shifted]; 
  StringSplit[
    StringJoin[
     Table[{Differences[shifted["Times"]][[i]], Mod[i, 2]}, {i, 
        Length@Differences[shifted["Times"]]}] /. {{x_, 1} /; .5 dit <
           x < 1.5 dit -> ".", {x_, 1} /; 2.5 dit < x < 3.5 dit -> 
        "-", {x_, 0} /; 2.5 dit < x < 3.5 dit -> 
        "/", {x_, 0} /; .5 dit < x < 1.5 dit -> 
        Nothing, {x_, 0} /; 5 dit < x < 12 dit -> "/_/"}], "/"] /. 
   "_" -> " "
  ]


UmbrellaOther`decodeMorse[wav_String] :=
 Module[{decoded},
	decoded = decodeMorseSignal[Import["E:/kernel/morse/" <> wav <> ".wav"]];
	Print[StringJoin[decoded /. inversecode]]
 ]
  
  
(* set time constrained at 60s *) 
 
SetAttributes[timecon, HoldAll]
timecon[new_] := TimeConstrained[new, 60]
$Pre = timecon;

End[]

EndPackage[]