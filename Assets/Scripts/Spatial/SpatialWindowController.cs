using UnityEngine;

public sealed class SpatialWindowController : MonoBehaviour
{
    public SpatialWindowState State { get; private set; } = new SpatialWindowState(0f, 0f, 2f, 1f);

    public void Apply(SpatialWindowState state)
    {
        State = new SpatialWindowState(
            Mathf.Clamp(state.X, -2f, 2f),
            Mathf.Clamp(state.Y, -1.5f, 1.5f),
            Mathf.Clamp(state.Z, 0.5f, 8f),
            Mathf.Clamp(state.Scale, 0.25f, 4f));
        transform.localPosition = new Vector3(State.X, State.Y, State.Z);
        transform.localScale = Vector3.one * State.Scale;
    }

    public void Center(float z = 2f) => Apply(new SpatialWindowState(0f, 0f, z, 1f));
}
