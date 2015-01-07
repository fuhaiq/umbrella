(* ::Package:: *)

(* :Title: Umbrella *)

(* :Author: fuhaiq@gmail.com*)

BeginPackage["Umbrella`"]

Umbrella::usage = "Evaluate expression to MathMLForm or GIF";

Begin["`Private`"]


Needs["WorldPlot`"]

Umbrella[dir_, expr_] := Module[{result, expression},
	{expression = ToExpression[expr]}
	If[MemberQ[expression, _Graphics | _Graphics3D | _Graph | _Manipulate | _WorldGraphics, {0, \[Infinity]}],
	result = CreateUUID[] <> ".BMP";
	Export[dir <> result, expression, "BMP"];
	,
	result = ToString[expression, FormatType -> MathMLForm, PageWidth -> \[Infinity]];
	];
	Return[result];
];

End[]

EndPackage[]
