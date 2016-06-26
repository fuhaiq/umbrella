(* ::Package:: *)

(* :Title: Umbrella *)

(* :Author: fuhaiq@gmail.com*)

BeginPackage["Umbrella`", {"JLink`", "MSP`"}]

Umbrella::usage = "Evaluate expression to GIF";

Begin["`Private`"]

MSP`Utility`SetSecurity["c:/Users/administrator/SecurityConfiguration.m"]

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
