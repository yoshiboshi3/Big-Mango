#if UNITY_EDITOR && UNITY_ANDROID
using System.IO;
using UnityEditor.Android;
using UnityEngine;

public sealed class ShizukuGradlePostprocessor : IPostGenerateGradleAndroidProject
{
    public int callbackOrder => 1000;

    public void OnPostGenerateGradleAndroidProject(string path)
    {
        var gradlePath = Path.Combine(path, "build.gradle");
        var text = File.ReadAllText(gradlePath);
        const string marker = "dependencies {";
        int index = text.IndexOf(marker, System.StringComparison.Ordinal);
        if (index < 0)
        {
            Debug.LogError("Scottish Targe: could not find dependencies block in unityLibrary/build.gradle");
            return;
        }
        int insert = index + marker.Length;
        const string dependencies = @"
    implementation 'dev.rikka.shizuku:api:13.1.5'
    implementation 'dev.rikka.shizuku:provider:13.1.5'
";
        if (!text.Contains("dev.rikka.shizuku:api:13.1.5"))
        {
            text = text.Insert(insert, dependencies);
            File.WriteAllText(gradlePath, text);
            Debug.Log("Scottish Targe: injected Shizuku Maven dependencies into unityLibrary.");
        }
    }
}
#endif
