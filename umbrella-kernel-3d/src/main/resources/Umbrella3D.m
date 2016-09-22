(* ::Package:: *)

(* :Title: Umbrella *)

(* :Author: fuhaiq@gmail.com*)

BeginPackage["Umbrella3D`", {"JLink`", "MSP`"}]

Umbrella::usage = "Evaluate expression to GIF and x3d";

Begin["`Private`"]

MSP`Utility`SetSecurity["c:/Users/administrator/SecurityConfiguration.m"]

$Pre = Function[expr, 
	Module[
		{expr$ = ToString[Unevaluated[expr], InputForm]},
		
		$$result = TimeConstrained[MSPToExpression[expr$], 60];

		If[SameQ[$$result, $Aborted], Abort[]];

		If[UnsameQ[$$result, Null], 
			If[MatchQ[$$result, _Graphics3D],
				uuid = CreateUUID[];
				Export["E:/kernel/temp/" <> uuid <> ".x3d", $$result];
				Print["4496c5c3-269e-43be-a94b-19a523e8dcf5" <> uuid]
			, 
				LinkWrite[$ParentLink, DisplayPacket[EvaluateToTypeset[$$result]]]
			]
			
		
		];		
	],
	
	HoldAllComplete
]

End[]

EndPackage[]
