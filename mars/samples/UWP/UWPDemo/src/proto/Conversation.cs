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
    public static partial class Conversation {
    
      #region Extension registration
      public static void RegisterAllExtensions(pb::ExtensionRegistry registry) {
      }
      #endregion
      #region Static variables
      #endregion
      #region Extensions
      internal static readonly object Descriptor;
      static Conversation() {
        Descriptor = null;
      }
      #endregion
      
    }
  }
  #region Messages
  [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
  [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
  [global::System.CodeDom.Compiler.GeneratedCodeAttribute("ProtoGen", "2.3.0.277")]
  public sealed partial class Conversation : pb::GeneratedMessageLite<Conversation, Conversation.Builder> {
    private Conversation() { }
    private static readonly Conversation defaultInstance = new Conversation().MakeReadOnly();
    private static readonly string[] _conversationFieldNames = new string[] { "name", "notice", "topic" };
    private static readonly uint[] _conversationFieldTags = new uint[] { 26, 18, 10 };
    public static Conversation DefaultInstance {
      get { return defaultInstance; }
    }
    
    public override Conversation DefaultInstanceForType {
      get { return DefaultInstance; }
    }
    
    protected override Conversation ThisMessage {
      get { return this; }
    }
    
    public const int TopicFieldNumber = 1;
    private bool hasTopic;
    private string topic_ = "";
    public string Topic {
      get { return topic_; }
    }
    
    public const int NoticeFieldNumber = 2;
    private bool hasNotice;
    private string notice_ = "";
    public string Notice {
      get { return notice_; }
    }
    
    public const int NameFieldNumber = 3;
    private bool hasName;
    private string name_ = "";
    public string Name {
      get { return name_; }
    }
    
    public override bool IsInitialized {
      get {
        if (!hasTopic) return false;
        if (!hasNotice) return false;
        if (!hasName) return false;
        return true;
      }
    }
    
    public override void WriteTo(pb::ICodedOutputStream output) {
      int size = SerializedSize;
      string[] field_names = _conversationFieldNames;
      if (hasTopic) {
        output.WriteString(1, field_names[2], Topic);
      }
      if (hasNotice) {
        output.WriteString(2, field_names[1], Notice);
      }
      if (hasName) {
        output.WriteString(3, field_names[0], Name);
      }
    }
    
    private int memoizedSerializedSize = -1;
    public override int SerializedSize {
      get {
        int size = memoizedSerializedSize;
        if (size != -1) return size;
        
        size = 0;
        if (hasTopic) {
          size += pb::CodedOutputStream.ComputeStringSize(1, Topic);
        }
        if (hasNotice) {
          size += pb::CodedOutputStream.ComputeStringSize(2, Notice);
        }
        if (hasName) {
          size += pb::CodedOutputStream.ComputeStringSize(3, Name);
        }
        memoizedSerializedSize = size;
        return size;
      }
    }
    
    #region Lite runtime methods
    public override int GetHashCode() {
      int hash = GetType().GetHashCode();
      if (hasTopic) hash ^= topic_.GetHashCode();
      if (hasNotice) hash ^= notice_.GetHashCode();
      if (hasName) hash ^= name_.GetHashCode();
      return hash;
    }
    
    public override bool Equals(object obj) {
      Conversation other = obj as Conversation;
      if (other == null) return false;
      if (hasTopic != other.hasTopic || (hasTopic && !topic_.Equals(other.topic_))) return false;
      if (hasNotice != other.hasNotice || (hasNotice && !notice_.Equals(other.notice_))) return false;
      if (hasName != other.hasName || (hasName && !name_.Equals(other.name_))) return false;
      return true;
    }
    
    public override void PrintTo(global::System.IO.TextWriter writer) {
      PrintField("topic", hasTopic, topic_, writer);
      PrintField("notice", hasNotice, notice_, writer);
      PrintField("name", hasName, name_, writer);
    }
    #endregion
    
    public static Conversation ParseFrom(byte[] data) {
      return ((Builder) CreateBuilder().MergeFrom(data)).BuildParsed();
    }
    private Conversation MakeReadOnly() {
      return this;
    }
    
    public static Builder CreateBuilder() { return new Builder(); }
    public override Builder ToBuilder() { return CreateBuilder(this); }
    public override Builder CreateBuilderForType() { return new Builder(); }
    public static Builder CreateBuilder(Conversation prototype) {
      return new Builder(prototype);
    }
    
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
    [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
    [global::System.CodeDom.Compiler.GeneratedCodeAttribute("ProtoGen", "2.3.0.277")]
    public sealed partial class Builder : pb::GeneratedBuilderLite<Conversation, Builder> {
      protected override Builder ThisBuilder {
        get { return this; }
      }
      public Builder() {
        result = DefaultInstance;
        resultIsReadOnly = true;
      }
      internal Builder(Conversation cloneFrom) {
        result = cloneFrom;
        resultIsReadOnly = true;
      }
      
      private bool resultIsReadOnly;
      private Conversation result;
      
      private Conversation PrepareBuilder() {
        if (resultIsReadOnly) {
          Conversation original = result;
          result = new Conversation();
          resultIsReadOnly = false;
          MergeFrom(original);
        }
        return result;
      }
      
      public override bool IsInitialized {
        get { return result.IsInitialized; }
      }
      
      protected override Conversation MessageBeingBuilt {
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
      
      public override Conversation DefaultInstanceForType {
        get { return global::com.tencent.mars.sample.proto.Conversation.DefaultInstance; }
      }
      
      public override Conversation BuildPartial() {
        if (resultIsReadOnly) {
          return result;
        }
        resultIsReadOnly = true;
        return result.MakeReadOnly();
      }
      
      public override Builder MergeFrom(pb::IMessageLite other) {
        if (other is Conversation) {
          return MergeFrom((Conversation) other);
        } else {
          base.MergeFrom(other);
          return this;
        }
      }
      
      public override Builder MergeFrom(Conversation other) {
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
            int field_ordinal = global::System.Array.BinarySearch(_conversationFieldNames, field_name, global::System.StringComparer.Ordinal);
            if(field_ordinal >= 0)
              tag = _conversationFieldTags[field_ordinal];
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
              result.hasTopic = input.ReadString(ref result.topic_);
              break;
            }
            case 18: {
              result.hasNotice = input.ReadString(ref result.notice_);
              break;
            }
            case 26: {
              result.hasName = input.ReadString(ref result.name_);
              break;
            }
          }
        }
        
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
        
        public string Notice {
          get { return result.Notice; }
          set { SetNotice(value); }
        }
        public Builder SetNotice(string value) {
          pb::ThrowHelper.ThrowIfNull(value, "value");
          PrepareBuilder();
          result.hasNotice = true;
          result.notice_ = value;
          return this;
        }
        public Builder ClearNotice() {
          PrepareBuilder();
          result.hasNotice = false;
          result.notice_ = "";
          return this;
        }
        
        public string Name {
          get { return result.Name; }
          set { SetName(value); }
        }
        public Builder SetName(string value) {
          pb::ThrowHelper.ThrowIfNull(value, "value");
          PrepareBuilder();
          result.hasName = true;
          result.name_ = value;
          return this;
        }
        public Builder ClearName() {
          PrepareBuilder();
          result.hasName = false;
          result.name_ = "";
          return this;
        }
      }
      static Conversation() {
        object.ReferenceEquals(global::com.tencent.mars.sample.proto.Proto.Conversation.Descriptor, null);
      }
    }
    
    #endregion
    
  }
  
  #endregion Designer generated code
