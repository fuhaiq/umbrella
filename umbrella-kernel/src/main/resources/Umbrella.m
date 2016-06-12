(* ::Package:: *)

(* :Title: Umbrella *)

(* :Author: fuhaiq@gmail.com*)

BeginPackage["Umbrella`"]

Umbrella::usage = "Evaluate expression to MathMLForm or GIF";

Begin["`Private`"]

Needs["JLink`"]

Needs["MSP`"]

MSP`Utility`SetSecurity["c:/Users/Administrator/SecurityConfiguration.m"]

$Pre = Function[expr, 
	Module[
		{expr$ = ToString[Unevaluated[expr], InputForm]},
		
		$$result = TimeConstrained[MSPToExpression[expr$], 60];

		If[SameQ[$$result, $Aborted], Abort[]];

		If[UnsameQ[$$result, Null], LinkWrite[$ParentLink, DisplayPacket[EvaluateToTypeset[$$result]]]];		
	],
	
	HoldAllComplete
]

End[]

EndPackage[]
