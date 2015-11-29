(* ::Package:: *)

(* :Title: Umbrella *)

(* :Author: fuhaiq@gmail.com*)

BeginPackage["Umbrella`"]

Umbrella::usage = "Evaluate expression to MathMLForm or GIF";

Begin["`Private`"]

Needs["JLink`"]

Needs["MSP`"]

MSP`Utility`SetSecurity["/home/wesker/SecurityConfiguration.m"]

$Pre = Function[expr, 
	Module[
		{expr$ = ToString[Unevaluated[expr], InputForm]},
		
		$$result = TimeConstrained[MSPToExpression[expr$], 20];

		If[MemberQ[$$result, _Graphics | _Graphics3D | _Graph | _Manipulate | _Rotate, {0, \[Infinity]}], 
			LinkWrite[$ParentLink, DisplayPacket[EvaluateToTypeset[$$result]]]
			, 
			MathMLForm[$$result]
		]
	],
	
	HoldAllComplete
]

End[]

EndPackage[]
