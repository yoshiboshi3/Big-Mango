using UnityEngine;
using UnityEngine.XR;

public sealed class HeadPoseDriver : MonoBehaviour
{
    void LateUpdate()
    {
        transform.localPosition = InputTracking.GetLocalPosition(XRNode.CenterEye);
        transform.localRotation = InputTracking.GetLocalRotation(XRNode.CenterEye);
    }
}
