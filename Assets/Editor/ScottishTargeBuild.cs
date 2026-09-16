#if UNITY_EDITOR
using System;
using System.IO;
using UnityEditor;
using UnityEditor.Build;
using UnityEditor.Build.Reporting;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.SceneManagement;

public static class ScottishTargeBuild
{
    public const string ScenePath = "Assets/Scenes/Proof.unity";
    public const string ApkPath = "Builds/Scottish-Targe-debug.apk";

    [MenuItem("Scottish Targe/Create Proof Scene")]
    public static void CreateProofScene()
    {
        Directory.CreateDirectory("Assets/Scenes");
        var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);

        var cameraObject = new GameObject("XREAL Head Camera");
        cameraObject.tag = "MainCamera";
        var camera = cameraObject.AddComponent<Camera>();
        camera.nearClipPlane = 0.05f;
        camera.farClipPlane = 100f;
        camera.clearFlags = CameraClearFlags.SolidColor;
        camera.backgroundColor = Color.black;
        cameraObject.AddComponent<HeadPoseDriver>();

        var windowRoot = new GameObject("Hosted Android App Window Root");
        windowRoot.transform.position = new Vector3(0f, 0f, 2f);
        var spatial = windowRoot.AddComponent<SpatialWindowController>();

        var panel = GameObject.CreatePrimitive(PrimitiveType.Quad);
        panel.name = "Hosted Android App Surface";
        panel.transform.SetParent(windowRoot.transform, false);
        panel.transform.localScale = new Vector3(1.6f, 0.9f, 1f);
        var renderer = panel.GetComponent<MeshRenderer>();
        var shader = Shader.Find("Unlit/Texture") ?? Shader.Find("Sprites/Default");
        renderer.sharedMaterial = new Material(shader) { name = "Hosted App Material" };

        var proof = new GameObject("Scottish Targe Proof Controller");
        var frames = proof.AddComponent<HostedAppFrameSource>();
        frames.TargetRenderer = renderer;
        var controller = proof.AddComponent<ProofController>();
        controller.Window = spatial;
        controller.Frames = frames;

        EditorSceneManager.SaveScene(scene, ScenePath);
        EditorBuildSettings.scenes = new[] { new EditorBuildSettingsScene(ScenePath, true) };
        AssetDatabase.SaveAssets();
        Debug.Log("Scottish Targe proof scene created: " + ScenePath);
    }

    [MenuItem("Scottish Targe/Configure Android")]
    public static void ConfigureAndroid()
    {
        PlayerSettings.companyName = "Scottish Targe";
        PlayerSettings.productName = "Scottish Targe";
        PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, "com.xrealcanvas.scottishtarge");
        PlayerSettings.bundleVersion = "0.1-proof";
        PlayerSettings.Android.bundleVersionCode = 1;
        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel29;
        PlayerSettings.SetScriptingBackend(NamedBuildTarget.Android, ScriptingImplementation.IL2CPP);
        PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
        PlayerSettings.SetGraphicsAPIs(BuildTarget.Android, new[] { GraphicsDeviceType.OpenGLES3 });
        PlayerSettings.stripEngineCode = false;
        EditorUserBuildSettings.androidBuildSystem = AndroidBuildSystem.Gradle;
        EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTargetGroup.Android, BuildTarget.Android);
        AssetDatabase.SaveAssets();
        Debug.Log("Scottish Targe Android settings configured.");
    }

    public static void BuildAndroid()
    {
        ConfigureAndroid();
        CreateProofScene();
        Directory.CreateDirectory("Builds");
        var options = new BuildPlayerOptions
        {
            scenes = new[] { ScenePath },
            locationPathName = ApkPath,
            target = BuildTarget.Android,
            targetGroup = BuildTargetGroup.Android,
            options = BuildOptions.Development
        };
        BuildReport report = BuildPipeline.BuildPlayer(options);
        if (report.summary.result != BuildResult.Succeeded)
            throw new Exception($"Scottish Targe build failed: {report.summary.result} ({report.summary.totalErrors} errors)");
        Debug.Log($"Scottish Targe APK: {Path.GetFullPath(ApkPath)} ({report.summary.totalSize} bytes)");
    }
}
#endif
