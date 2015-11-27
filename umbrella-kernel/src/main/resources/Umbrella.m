(* ::Package:: *)

(* :Title: Umbrella *)

(* :Author: fuhaiq@gmail.com*)

BeginPackage["Umbrella`"]

Umbrella::usage = "Evaluate expression to MathMLForm or GIF";

Begin["`Private`"]

Needs["JLink`"]

(* Set MathMLForm *)

$PrePrint = With[{expr = #},If[MemberQ[expr, _Graphics | _Graphics3D | _Graph | _Manipulate | _Rotate, {0, \[Infinity]}], LinkWrite[$ParentLink, DisplayPacket[EvaluateToTypeset[expr]]], MathMLForm[expr]]] &;


(*

Needs["MSP`"]

MSP`Utility`SetSecurity[ "SecurityConfiguration.m"];

$Pre = Function[expr, 
      Module[
        {expr$ = ToString[Unevaluated[expr], InputForm]},
         MSPToExpression[expr$]
  ],
  HoldAll
]
*)

(* Set time constrained *)
SetAttributes[timecon, HoldAll]
timecon[new_] := TimeConstrained[new, 10]
$Pre = timecon;

End[]

EndPackage[]
