// Generated by ProtoGen, Version=2.3.0.277, Culture=neutral, PublicKeyToken=17b3b1f090c3ea48.  DO NOT EDIT!
#pragma warning disable 1591, 0612
#region Designer generated code

using pb = global::Google.ProtocolBuffers;
using pbc = global::Google.ProtocolBuffers.Collections;
using pbd = global::Google.ProtocolBuffers.Descriptors;
using scg = global::System.Collections.Generic;
namespace com.tencent.mars.sample.proto {
  
  namespace Proto {
    
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
    [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
    [global::System.CodeDom.Compiler.GeneratedCodeAttribute("ProtoGen", "2.3.0.277")]
    public static partial class SendMessageRequest {
    
      #region Extension registration
      public static void RegisterAllExtensions(pb::ExtensionRegistry registry) {
      }
      #endregion
      #region Static variables
      #endregion
      #region Extensions
      internal static readonly object Descriptor;
      static SendMessageRequest() {
        Descriptor = null;
      }
      #endregion
      
    }
  }
  #region Messages
  [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
  [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
  [global::System.CodeDom.Compiler.GeneratedCodeAttribute("ProtoGen", "2.3.0.277")]
  public sealed partial class SendMessageRequest : pb::GeneratedMessageLite<SendMessageRequest, SendMessageRequest.Builder> {
    private SendMessageRequest() { }
    private static readonly SendMessageRequest defaultInstance = new SendMessageRequest().MakeReadOnly();
    private static readonly string[] _sendMessageRequestFieldNames = new string[] { "access_token", "from", "text", "to", "topic" };
    private static readonly uint[] _sendMessageRequestFieldTags = new uint[] { 10, 18, 34, 26, 42 };
    public static SendMessageRequest DefaultInstance {
      get { return defaultInstance; }
    }
    
    public override SendMessageRequest DefaultInstanceForType {
      get { return DefaultInstance; }
    }
    
    protected override SendMessageRequest ThisMessage {
      get { return this; }
    }
    
    public const int AccessTokenFieldNumber = 1;
    private bool hasAccessToken;
    private string accessToken_ = "";
    public string AccessToken {
      get { return accessToken_; }
    }
    
    public const int FromFieldNumber = 2;
    private bool hasFrom;
    private string from_ = "";
    public string From {
      get { return from_; }
    }
    
    public const int ToFieldNumber = 3;
    private bool hasTo;
    private string to_ = "";
    public string To {
      get { return to_; }
    }
    
    public const int TextFieldNumber = 4;
    private bool hasText;
    private string text_ = "";
    public string Text {
      get { return text_; }
    }
    
    public const int TopicFieldNumber = 5;
    private bool hasTopic;
    private string topic_ = "";
    public string Topic {
      get { return topic_; }
    }
    
    public override bool IsInitialized {
      get {
        if (!hasAccessToken) return false;
        if (!hasFrom) return false;
        if (!hasTo) return false;
        if (!hasText) return false;
        if (!hasTopic) return false;
        return true;
      }
    }
    
    public override void WriteTo(pb::ICodedOutputStream output) {
      int size = SerializedSize;
      string[] field_names = _sendMessageRequestFieldNames;
      if (hasAccessToken) {
        output.WriteString(1, field_names[0], AccessToken);
      }
      if (hasFrom) {
        output.WriteString(2, field_names[1], From);
      }
      if (hasTo) {
        output.WriteString(3, field_names[3], To);
      }
      if (hasText) {
        output.WriteString(4, field_names[2], Text);
      }
      if (hasTopic) {
        output.WriteString(5, field_names[4], Topic);
      }
    }
    
    private int memoizedSerializedSize = -1;
    public override int SerializedSize {
      get {
        int size = memoizedSerializedSize;
        if (size != -1) return size;
        
        size = 0;
        if (hasAccessToken) {
          size += pb::CodedOutputStream.ComputeStringSize(1, AccessToken);
        }
        if (hasFrom) {
          size += pb::CodedOutputStream.ComputeStringSize(2, From);
        }
        if (hasTo) {
          size += pb::CodedOutputStream.ComputeStringSize(3, To);
        }
        if (hasText) {
          size += pb::CodedOutputStream.ComputeStringSize(4, Text);
        }
        if (hasTopic) {
          size += pb::CodedOutputStream.ComputeStringSize(5, Topic);
        }
        memoizedSerializedSize = size;
        return size;
      }
    }
    
    #region Lite runtime methods
    public override int GetHashCode() {
      int hash = GetType().GetHashCode();
      if (hasAccessToken) hash ^= accessToken_.GetHashCode();
      if (hasFrom) hash ^= from_.GetHashCode();
      if (hasTo) hash ^= to_.GetHashCode();
      if (hasText) hash ^= text_.GetHashCode();
      if (hasTopic) hash ^= topic_.GetHashCode();
      return hash;
    }
    
    public override bool Equals(object obj) {
      SendMessageRequest other = obj as SendMessageRequest;
      if (other == null) return false;
      if (hasAccessToken != other.hasAccessToken || (hasAccessToken && !accessToken_.Equals(other.accessToken_))) return false;
      if (hasFrom != other.hasFrom || (hasFrom && !from_.Equals(other.from_))) return false;
      if (hasTo != other.hasTo || (hasTo && !to_.Equals(other.to_))) return false;
      if (hasText != other.hasText || (hasText && !text_.Equals(other.text_))) return false;
      if (hasTopic != other.hasTopic || (hasTopic && !topic_.Equals(other.topic_))) return false;
      return true;
    }
    
    public override void PrintTo(global::System.IO.TextWriter writer) {
      PrintField("access_token", hasAccessToken, accessToken_, writer);
      PrintField("from", hasFrom, from_, writer);
      PrintField("to", hasTo, to_, writer);
      PrintField("text", hasText, text_, writer);
      PrintField("topic", hasTopic, topic_, writer);
    }
    #endregion
    
    public static SendMessageRequest ParseFrom(byte[] data) {
      return ((Builder) CreateBuilder().MergeFrom(data)).BuildParsed();
    }
    private SendMessageRequest MakeReadOnly() {
      return this;
    }
    
    public static Builder CreateBuilder() { return new Builder(); }
    public override Builder ToBuilder() { return CreateBuilder(this); }
    public override Builder CreateBuilderForType() { return new Builder(); }
    public static Builder CreateBuilder(SendMessageRequest prototype) {
      return new Builder(prototype);
    }
    
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
    [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
    [global::System.CodeDom.Compiler.GeneratedCodeAttribute("ProtoGen", "2.3.0.277")]
    public sealed partial class Builder : pb::GeneratedBuilderLite<SendMessageRequest, Builder> {
      protected override Builder ThisBuilder {
        get { return this; }
      }
      public Builder() {
        result = DefaultInstance;
        resultIsReadOnly = true;
      }
      internal Builder(SendMessageRequest cloneFrom) {
        result = cloneFrom;
        resultIsReadOnly = true;
      }
      
      private bool resultIsReadOnly;
      private SendMessageRequest result;
      
      private SendMessageRequest PrepareBuilder() {
        if (resultIsReadOnly) {
          SendMessageRequest original = result;
          result = new SendMessageRequest();
          resultIsReadOnly = false;
          MergeFrom(original);
        }
        return result;
      }
      
      public override bool IsInitialized {
        get { return result.IsInitialized; }
      }
      
      protected override SendMessageRequest MessageBeingBuilt {
        get { return PrepareBuilder(); }
      }
      
      public override Builder Clear() {
        result = DefaultInstance;
        resultIsReadOnly = true;
        return this;
      }
      
      public override Builder Clone() {
        if (resultIsReadOnly) {
          return new Builder(result);
        } else {
          return new Builder().MergeFrom(result);
        }
      }
      
      public override SendMessageRequest DefaultInstanceForType {
        get { return global::com.tencent.mars.sample.proto.SendMessageRequest.DefaultInstance; }
      }
      
      public override SendMessageRequest BuildPartial() {
        if (resultIsReadOnly) {
          return result;
        }
        resultIsReadOnly = true;
        return result.MakeReadOnly();
      }
      
      public override Builder MergeFrom(pb::IMessageLite other) {
        if (other is SendMessageRequest) {
          return MergeFrom((SendMessageRequest) other);
        } else {
          base.MergeFrom(other);
          return this;
        }
      }
      
      public override Builder MergeFrom(SendMessageRequest other) {
      return this;
      }
      
      public override Builder MergeFrom(pb::ICodedInputStream input) {
        return MergeFrom(input, pb::ExtensionRegistry.Empty);
      }
      
      public override Builder MergeFrom(pb::ICodedInputStream input, pb::ExtensionRegistry extensionRegistry) {
        PrepareBuilder();
        uint tag;
        string field_name;
        while (input.ReadTag(out tag, out field_name)) {
          if(tag == 0 && field_name != null) {
            int field_ordinal = global::System.Array.BinarySearch(_sendMessageRequestFieldNames, field_name, global::System.StringComparer.Ordinal);
            if(field_ordinal >= 0)
              tag = _sendMessageRequestFieldTags[field_ordinal];
            else {
              ParseUnknownField(input, extensionRegistry, tag, field_name);
              continue;
            }
          }
          switch (tag) {
            case 0: {
              throw pb::InvalidProtocolBufferException.InvalidTag();
            }
            default: {
              if (pb::WireFormat.IsEndGroupTag(tag)) {
                return this;
              }
              ParseUnknownField(input, extensionRegistry, tag, field_name);
              break;
            }
            case 10: {
              result.hasAccessToken = input.ReadString(ref result.accessToken_);
              break;
            }
            case 18: {
              result.hasFrom = input.ReadString(ref result.from_);
              break;
            }
            case 26: {
              result.hasTo = input.ReadString(ref result.to_);
              break;
            }
            case 34: {
              result.hasText = input.ReadString(ref result.text_);
              break;
            }
            case 42: {
              result.hasTopic = input.ReadString(ref result.topic_);
              break;
            }
          }
        }
        
        return this;
        }
        
        
        public string AccessToken {
          get { return result.AccessToken; }
          set { SetAccessToken(value); }
        }
        public Builder SetAccessToken(string value) {
          pb::ThrowHelper.ThrowIfNull(value, "value");
          PrepareBuilder();
          result.hasAccessToken = true;
          result.accessToken_ = value;
          return this;
        }
        public Builder ClearAccessToken() {
          PrepareBuilder();
          result.hasAccessToken = false;
          result.accessToken_ = "";
          return this;
        }
        
        public string From {
          get { return result.From; }
          set { SetFrom(value); }
        }
        public Builder SetFrom(string value) {
          pb::ThrowHelper.ThrowIfNull(value, "value");
          PrepareBuilder();
          result.hasFrom = true;
          result.from_ = value;
          return this;
        }
        public Builder ClearFrom() {
          PrepareBuilder();
          result.hasFrom = false;
          result.from_ = "";
          return this;
        }
        
        public string To {
          get { return result.To; }
          set { SetTo(value); }
        }
        public Builder SetTo(string value) {
          pb::ThrowHelper.ThrowIfNull(value, "value");
          PrepareBuilder();
          result.hasTo = true;
          result.to_ = value;
          return this;
        }
        public Builder ClearTo() {
          PrepareBuilder();
          result.hasTo = false;
          result.to_ = "";
          return this;
        }
        
        public string Text {
          get { return result.Text; }
          set { SetText(value); }
        }
        public Builder SetText(string value) {
          pb::ThrowHelper.ThrowIfNull(value, "value");
          PrepareBuilder();
          result.hasText = true;
          result.text_ = value;
          return this;
        }
        public Builder ClearText() {
          PrepareBuilder();
          result.hasText = false;
          result.text_ = "";
          return this;
        }
        
        public string Topic {
          get { return result.Topic; }
          set { SetTopic(value); }
        }
        public Builder SetTopic(string value) {
          pb::ThrowHelper.ThrowIfNull(value, "value");
          PrepareBuilder();
          result.hasTopic = true;
          result.topic_ = value;
          return this;
        }
        public Builder ClearTopic() {
          PrepareBuilder();
          result.hasTopic = false;
          result.topic_ = "";
          return this;
        }
      }
      static SendMessageRequest() {
        object.ReferenceEquals(global::com.tencent.mars.sample.proto.Proto.SendMessageRequest.Descriptor, null);
      }
    }
    
    #endregion
    
  }
  
  #endregion Designer generated code
